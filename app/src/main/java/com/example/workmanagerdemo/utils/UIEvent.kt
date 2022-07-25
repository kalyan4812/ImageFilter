package com.example.workmanagerdemo.utils

sealed class UIEvent {

    data class showSnackBar(val messsage:String,val action:String?=null):UIEvent()

    data class showToast(val messsage: String):UIEvent()
}