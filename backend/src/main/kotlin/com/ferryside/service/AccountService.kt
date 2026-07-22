package com.ferryside.service

import com.baomidou.mybatisplus.extension.service.IService
import com.ferryside.entity.auth.Account

/**
@ Author: ferry
@ Date 22/07/2026 06:59
 */
interface AccountService: IService<Account> {
    fun findAccountByUsernameOrEmail(usernameOrEmail: String): Account?
}