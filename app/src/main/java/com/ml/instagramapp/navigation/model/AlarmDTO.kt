package com.ml.instagramapp.navigation.model

data class AlarmDTO(
    var destinationUid : String? = null,
    var userId : String? = null,
    var uid : String? = null,

    //0 : 좋아요 알람
    //1 : comment 알람
    //2 : 팔로우 알람
    var kind : Int? = null,
    var message : String? = null,
    var timestamp : Long? = null
)