package com.ferryside.entity

import com.baomidou.mybatisplus.annotation.TableName

/**
@ Author: ferry
@ Date 29/06/2026 17:59
 */
@TableName("users")
data class Users(
    var id: Long? = null,
    var username: String,
    var password: String? = null,
    var email: String? = null,
)