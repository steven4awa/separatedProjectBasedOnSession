package com.ferryside.handler

import com.ferryside.entity.RestBean
import com.ferryside.util.JsonUtil
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.stereotype.Component

/**
@ Author: ferry
@ Date 28/06/2026 13:00
 */
@Component
class LoginSuccessHandler : AuthenticationSuccessHandler {
    override fun onAuthenticationSuccess(request: HttpServletRequest,
                                         response: HttpServletResponse,
                                         chain: FilterChain,
                                         authentication: Authentication
    ) {}

    override fun onAuthenticationSuccess(request: HttpServletRequest,
                                         response: HttpServletResponse,
                                         authentication: Authentication
    ) {
        response.characterEncoding = Charsets.UTF_8.name()
        response.writer.write(JsonUtil.toJson(RestBean.success("login success")))
    }
}