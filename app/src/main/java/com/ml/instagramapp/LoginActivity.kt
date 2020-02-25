package com.ml.instagramapp

import android.content.Intent
import android.content.pm.PackageManager
import android.util.Base64
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
/*import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider*/
import kotlinx.android.synthetic.main.activity_login.*
import java.security.MessageDigest
import java.util.*

class LoginActivity : AppCompatActivity() {
    var auth : FirebaseAuth? = null //?를 해야 null을 담을수 있음...
    var googleSignInClient : GoogleSignInClient? = null
    var GOOGLE_LOGIN_CODE = 9001
    var callbackManager : CallbackManager? = null //facebook 로그인 결과값을 가지고오는 callback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        auth = FirebaseAuth.getInstance()
        email_login_button.setOnClickListener(){
            signinAndSignup()
        }
        google_sign_in_button.setOnClickListener(){
            googleLogin()
        }
        facebook_login_button.setOnClickListener(){
            facebookLogin()
        }

        var gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this,gso)
        callbackManager = CallbackManager.Factory.create()
        //generateSSHKey()
    }
    fun googleLogin(){
        var signInIntent = googleSignInClient?.signInIntent
        startActivityForResult(signInIntent,GOOGLE_LOGIN_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager?.onActivityResult(requestCode,resultCode,data)
        if(requestCode == GOOGLE_LOGIN_CODE){
            var result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
            if(result.isSuccess){
                var account = result.signInAccount
                firebaseAuthWithGoogle(account)
            }
        }
    }

    fun firebaseAuthWithGoogle(account: GoogleSignInAccount?){
        var credential = GoogleAuthProvider.getCredential(account?.idToken,null)
        auth?.signInWithCredential(credential)
            ?.addOnCompleteListener(){
                    task ->
                if(task.isSuccessful){//id가 생성되었을 때 작동
                    //Creating a user account
                    moveMainPage(task.result?.user)
                }else{
                    //show the error message
                    Toast.makeText(this, task.exception?.message, Toast.LENGTH_LONG).show()
                }
            }
    }

    fun signinAndSignup(){
        auth?.createUserWithEmailAndPassword(email_edittext.text.toString(),password_edittext.text.toString())?.addOnCompleteListener(){
                task ->
            if(task.isSuccessful){//id가 생성되었을 때 작동
                //Creating a user account
                moveMainPage(task.result?.user)
            }else if(task.exception?.message.isNullOrEmpty()){ //실패시 메세지 출력
                //Show the error message
                Toast.makeText(this, task.exception?.message, Toast.LENGTH_LONG).show()
            }else{ //로그인 하는 부분
                signinEmail()
            }
        }
    }

    fun signinEmail(){
        auth?.signInWithEmailAndPassword(email_edittext.text.toString(),password_edittext.text.toString())
            ?.addOnCompleteListener(){
                    task ->
                if(task.isSuccessful){//id가 생성되었을 때 작동
                    //Creating a user account
                    moveMainPage(task.result?.user)
                }else{
                    //show the error message
                    Toast.makeText(this, task.exception?.message, Toast.LENGTH_LONG).show()
                }
            }
    }

    //Firebase 유저 상태 넘겨줌
    fun moveMainPage(user: FirebaseUser?){
        if(user != null){ //유저상태가 있으면 메인액티비티 호출
            startActivity(Intent(this, MainActivity::class.java))
        }
    }



    fun facebookLogin(){
        //추가 권한 요청
        LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("public_profile","email"))
        LoginManager.getInstance().registerCallback(callbackManager, object : FacebookCallback<LoginResult> { //facebook 로그인에 성공했을때 넘어오는 부분
            override fun onSuccess(result: LoginResult?) {
                handleFacebookAccessToken(result?.accessToken)
            }
            override fun onError(error: FacebookException?) {

            }

            override fun onCancel() {

            }


        })
    }

    fun handleFacebookAccessToken(token: AccessToken?) {
        var credential = FacebookAuthProvider.getCredential(token?.token!!) //!!연산자는 널 값 보증(null값이 안들어오는걸 보증)
        auth?.signInWithCredential(credential)
            ?.addOnCompleteListener(){
                    task ->
                if(task.isSuccessful){//id가 생성되었을 때 작동
                    //Creating a user account
                    moveMainPage(task.result?.user)
                }else{
                    //show the error message
                    Toast.makeText(this, task.exception?.message, Toast.LENGTH_LONG).show()
                }
            }
    }











    fun generateSSHKey(){ //facebook hash 값 가지고오는 function
        try {
            val info = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            for (signature in info.signatures) {
                val md = MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())
                val hashKey = String(Base64.encode(md.digest(),0))
                Log.v("AppLog", "key: $hashKey")
            }
        } catch (e: Exception) {
            Log.e("AppLog", "error:", e)
        }
    }
}
