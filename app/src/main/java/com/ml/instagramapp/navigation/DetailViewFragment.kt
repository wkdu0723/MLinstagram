package com.ml.instagramapp.navigation

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Layout
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.FirebaseApiNotAvailableException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ml.instagramapp.R
import com.ml.instagramapp.navigation.model.AlarmDTO
import com.ml.instagramapp.navigation.model.ContentDTO
import com.ml.instagramapp.navigation.util.FcmPush
import kotlinx.android.synthetic.main.fragment_detail.view.*
import kotlinx.android.synthetic.main.fragment_user.view.*
import kotlinx.android.synthetic.main.item_detail.view.*

class DetailViewFragment : Fragment() {
    var firestore : FirebaseFirestore? = null
    var uid : String? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        var view = LayoutInflater.from(activity).inflate(R.layout.fragment_detail, container, false)
        firestore = FirebaseFirestore.getInstance()
        uid = FirebaseAuth.getInstance().currentUser?.uid

        view.detailviewfragment_recyclerview.adapter = DetailViewRecyclerViewAdapter()
        view.detailviewfragment_recyclerview.layoutManager = LinearLayoutManager(activity)
        return view
    }
    inner class DetailViewRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(){
        var contentDTOs : ArrayList<ContentDTO> = arrayListOf()
        var contentUidList : ArrayList<String> = arrayListOf()


        init{
            //db 쿼리 (실시간으로 반영되는 리스트나 채팅을 만들때 addSnapshotListener을 사용)
            firestore?.collection("images")?.orderBy("timestamp")?.addSnapshotListener{querySnapshot, firebaseFirestoreException ->
                contentDTOs.clear()
                contentUidList.clear()
                if(querySnapshot == null) return@addSnapshotListener

                for(snapshot in querySnapshot!!.documents){
                    var item = snapshot.toObject(ContentDTO::class.java)
                    contentDTOs.add(item!!)
                    contentUidList.add(snapshot.id)
                }
                notifyDataSetChanged()
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            var view = LayoutInflater.from(parent.context).inflate(R.layout.item_detail,parent,false)
            return CustomViewHolder(view)
        }

        inner class CustomViewHolder(view: View) : RecyclerView.ViewHolder(view)

        override fun getItemCount(): Int {
            return contentDTOs.size
        }


        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            var viewholder = (holder as CustomViewHolder).itemView

            //UserId
            viewholder.detailviewitem_profile_textview.text = contentDTOs!![position].userId

            firestore?.collection("profileImages")?.document(contentDTOs!![position].uid!!)?.addSnapshotListener{
                    documentSnapshot, firebaseFirestoreException ->
                if(documentSnapshot == null) return@addSnapshotListener
                if(documentSnapshot.data != null){
                    var url = documentSnapshot?.data!!["image"] //이미지 주소값을 받아옴
                    Glide.with(activity!!).load(url).apply(RequestOptions().circleCrop()).into(viewholder.detailviewitem_profile_image!!)
                }
            }

            //Image(이미지 url을 load하기위해 Glide사용)
            Glide.with(holder.itemView.context).load(contentDTOs!![position].imageUrl).into(viewholder.detailviewitem_imageview_content)

            //Explain of content
            viewholder.detailviewitem_explain_textview.text = contentDTOs!![position].explain

            //likes
            viewholder.detailviewitem_favoritecounter_textview.text ="Likes " + contentDTOs!![position].favoriteCount


            //ProfileImage

            //좋아요버튼 클릭 리스너
            viewholder.detailviewitem_favorite_imageview.setOnClickListener{
                favoriteEvent(position)
            }

            //좋아요 카운터와 하트색칠 이벤트
            if(contentDTOs!![position].favorites.containsKey(uid)){
                //클릭되었을 때
                viewholder.detailviewitem_favorite_imageview.setImageResource(R.drawable.ic_favorite)

            }else{
                viewholder.detailviewitem_favorite_imageview.setImageResource(R.drawable.ic_favorite_border)

            }

            //상대방 프로필을 클릭하면 상세페이지로 이동
            viewholder.detailviewitem_profile_image.setOnClickListener{
                var fragment = UserFragment()
                var bundle = Bundle()
                bundle.putString("destinationUid",contentDTOs[position].uid) //선택된 uid값을 받아옴 (UserFragment로 보내기 위함)
                bundle.putString("userId",contentDTOs[position].userId) //선택된 userId값을 받아옴 (UserFragment로 보내기 위함)
                fragment.arguments = bundle
                activity?.supportFragmentManager?.beginTransaction()?.replace(R.id.main_content,fragment)?.commit()
            }
            viewholder.detailviewitem_imageview_content_imageview.setOnClickListener{v ->
                var intent = Intent(v.context,CommentActivity::class.java)
                intent.putExtra("contentUid",contentUidList[position])
                intent.putExtra("destinationUid",contentDTOs[position].uid)
                startActivity(intent)

            }
        }

        fun favoriteEvent(position: Int){
            var tsDoc = firestore?.collection("images")?.document(contentUidList[position])
            firestore?.runTransaction{transaction ->
                var contentDTO = transaction.get(tsDoc!!).toObject(ContentDTO::class.java)

                //좋아요버튼이 클릭되어있을 경우
                if(contentDTO!!.favorites.containsKey(uid)){
                    //버튼 취소 이벤트
                    contentDTO?.favoriteCount = contentDTO?.favoriteCount - 1
                    contentDTO?.favorites.remove(uid)
                }else{
                    contentDTO?.favoriteCount = contentDTO?.favoriteCount + 1
                    contentDTO?.favorites[uid!!] = true
                    favoriteAlarm(contentDTOs[position].uid!!)

                }
                transaction.set(tsDoc, contentDTO)
            }
        }
        fun favoriteAlarm(destinationUid : String){
            var alarmDTO = AlarmDTO()
            alarmDTO.destinationUid = destinationUid
            alarmDTO.userId = FirebaseAuth.getInstance().currentUser?.email
            alarmDTO.uid = FirebaseAuth.getInstance().currentUser?.uid
            alarmDTO.kind = 0
            alarmDTO.timestamp = System.currentTimeMillis()
            FirebaseFirestore.getInstance().collection("alarms").document().set(alarmDTO)

            var message = FirebaseAuth.getInstance()?.currentUser?.email + getString(R.string.alarm_favorite)
            FcmPush.instance.sendMessage(destinationUid, "MLinstagram",message)
        }
    }
}
