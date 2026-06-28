package com.ferryside.util

import com.google.gson.Gson

/**
@ Author: ferry
@ Date 28/06/2026 13:06
 */
object JsonUtil {
    private val gson = Gson()

    fun toJson(any: Any): String {
        return gson.toJson(any)
    }

    fun <T> fromJson(json: String, clazz: Class<T>): T {
        return gson.fromJson(json, clazz)
    }
}