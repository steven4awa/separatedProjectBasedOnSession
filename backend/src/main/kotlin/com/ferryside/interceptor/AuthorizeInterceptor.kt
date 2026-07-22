package com.ferryside.interceptor

import com.ferryside.entity.auth.Account
import com.ferryside.service.AccountService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

/**
@ Author: ferry
@ Date 22/07/2026 06:26
 */
@Component
class AuthorizeInterceptor(
    private val accountService: AccountService
) : HandlerInterceptor{
    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any
    ): Boolean {
        val principal = (SecurityContextHolder.getContext().authentication?.principal).also {
            println(it)
        }
        var account: Account ?= null
        if (principal is UserDetails) {
            val username = principal.username
            account = accountService.findAccountByUsernameOrEmail(username)
        }

        request.session.setAttribute("account", account)
        return true
    }
}