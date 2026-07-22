package com.ferryside.entity.auth

import com.baomidou.mybatisplus.annotation.IdType
import com.baomidou.mybatisplus.annotation.TableId
import com.baomidou.mybatisplus.annotation.TableName

/**
@ Author: ferry
@ Date 22/07/2026 06:23
 */
@TableName("users")
data class Account(
    @TableId(type = IdType.AUTO)
    var id: Long? = null,
    var username: String,
    var email: String? = null,
)