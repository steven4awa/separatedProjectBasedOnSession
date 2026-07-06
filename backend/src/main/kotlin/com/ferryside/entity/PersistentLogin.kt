package com.ferryside.entity

import com.baomidou.mybatisplus.annotation.TableId
import com.baomidou.mybatisplus.annotation.TableName
import java.time.LocalDateTime

/**
@ Author: ferry
@ Date 06/07/2026 12:58
 */
@TableName("persistent_logins")
data class PersistentLogin(
    @TableId
    var series: String,
    var username: String,
    var token: String,
    var lastUsed: LocalDateTime
)
