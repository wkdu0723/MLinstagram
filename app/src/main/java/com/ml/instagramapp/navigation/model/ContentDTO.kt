package com.ml.instagramapp.navigation.model

data class ContentDTO(var explain : String? = null,
                      var imageUrl : String? = null,
                      var uid : String? = null,
                      var userId : String? = null,
                      var timestamp : Long? = null,
                      var favoriteCount : Int = 0,
                      //miutalbeMap(변경가능한 맵)
                      var favorites : MutableMap<String, Boolean> = HashMap()){
    data class  Comment(var uid : String? = null,
                        var userId : String? = null,
                        var comment : String? = null,
                        var timestamp : Long? = null)

}
