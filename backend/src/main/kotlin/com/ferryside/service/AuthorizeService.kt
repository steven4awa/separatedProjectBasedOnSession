package com.ferryside.service

import com.baomidou.mybatisplus.extension.service.IService
import com.ferryside.entity.Users
import jakarta.servlet.http.HttpSession
import org.springframework.security.core.userdetails.UserDetailsService

/**
@ Author: ferry
@ Date 29/06/2026 19:31
 */

interface AuthorizeService : UserDetailsService, IService<Users> {
    fun sendValidatedEmail(email: String): String
    fun validateAndRegister(username: String, password: String, email: String, code: String): String
}