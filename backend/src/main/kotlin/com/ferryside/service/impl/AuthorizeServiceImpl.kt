package com.ferryside.service.impl

import com.ferryside.service.AuthorizeService
import com.ferryside.service.UserService
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

/**
@ Author: ferry
@ Date 10/07/2026 16:10
 */
@Service
class AuthorizeServiceImpl(
    private val userService: UserService
) : AuthorizeService {
    override fun loadUserByUsername(username: String): UserDetails {
        val user = userService.findAccountByUsernameOrEmail(username)
            ?: throw UsernameNotFoundException("User not found")

        return User(
            user.username,
            user.password, // 必须是 String
            listOf() // 先不给权限，后面可以加 ROLE_USER
        )
    }

    override fun sendValidatedEmail(email: String): Boolean {
        return false
    }
}