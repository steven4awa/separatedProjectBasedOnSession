package com.ferryside.service.impl

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import com.ferryside.entity.Users
import com.ferryside.mapper.UsersMapper
import com.ferryside.service.UserService
import org.springframework.stereotype.Service

/**
@ Author: ferry
@ Date 29/06/2026 18:36
 */
@Service
class UserServiceImpl : UserService, ServiceImpl<UsersMapper, Users>(){
    override fun findAccountByUsernameOrEmail(usernameOrEmail: String): Users? {
        return   query()
            .eq("username", usernameOrEmail)
            .or()
            .eq("email", usernameOrEmail)
            .one()
    }
}