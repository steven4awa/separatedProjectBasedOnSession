package com.ferryside.handler

import com.ferryside.entity.RestBean
import com.ferryside.util.JsonUtil
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import org.springframework.stereotype.Component

/**
@ Author: ferry
@ Date 30/06/2026 18:47
 */
@Component
class LoginFailureHandler : AuthenticationFailureHandler {
    override fun onAuthenticationFailure(request: HttpServletRequest,
                                         response: HttpServletResponse,
                                         exception: AuthenticationException
    ) {
        response.characterEncoding = Charsets.UTF_8.name()
        response.writer.write(JsonUtil.toJson(RestBean.failure(401, exception.message)))
    }
}