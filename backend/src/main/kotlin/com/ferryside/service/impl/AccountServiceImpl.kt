package com.ferryside.service.impl

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import com.ferryside.entity.auth.Account
import com.ferryside.mapper.AccountMapper
import com.ferryside.service.AccountService
import org.springframework.stereotype.Service

/**
@ Author: ferry
@ Date 22/07/2026 06:59
 */
@Service
class AccountServiceImpl: ServiceImpl<AccountMapper, Account>(), AccountService {
    override fun findAccountByUsernameOrEmail(usernameOrEmail: String): Account? {
        return query()
            .eq("username", usernameOrEmail)
            .or()
            .eq("email", usernameOrEmail)
            .one()
    }
}