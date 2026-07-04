package com.ferryside.service

import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import org.springframework.security.core.userdetails.User

/**
@ Author: ferry
@ Date 29/06/2026 19:31
 */
@Service
class AuthorizeService(
    private val userService: UserService,
) : UserDetailsService{
    override fun loadUserByUsername(username: String): UserDetails {
//        println("loadUserByUsername $username, this method was invoked" +
//                "")
        val user = userService.findAccountByUsernameOrEmail(username)
            ?: throw UsernameNotFoundException("User not found")

        return User(
            user.username,
            user.password, // 必须是 String
            listOf() // 先不给权限，后面可以加 ROLE_USER
        )
    }
}