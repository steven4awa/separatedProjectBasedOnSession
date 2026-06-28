package com.ferryside.entity

/**
@ Author: ferry
@ Date 28/06/2026 12:10
 */
data class RestBean<T>(
    val status: Int,
    val success: Boolean,
    val message: T? = null,

){
    companion object{
        fun <T> success(): RestBean<T> {
            return RestBean(200,true,null)
        }

        fun <T> success(data: T): RestBean<T> {
            return RestBean(200,true,data)
        }

        fun <T> failure(status: Int): RestBean<T> {
            return RestBean(status,false,null)
        }

        fun <T> failure(status: Int, data: T): RestBean<T> {
            return RestBean(status,false,data)
        }
    }

}
