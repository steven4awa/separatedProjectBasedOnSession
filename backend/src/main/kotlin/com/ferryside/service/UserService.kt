package com.ferryside.service

import com.baomidou.mybatisplus.extension.service.IService
import com.ferryside.entity.Users

/**
@ Author: ferry
@ Date 29/06/2026 18:32
 */
interface UserService : IService<Users> {
    fun findAccountByUsernameOrEmail(usernameOrEmail: String): Users?
}