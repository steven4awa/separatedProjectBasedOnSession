package com.ferryside.handler

import com.ferryside.entity.RestBean
import com.ferryside.util.JsonUtil
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component

/**
@ Author: ferry
@ Date 15/07/2026
 */
@Component
class UnauthorizedHandler : AuthenticationEntryPoint {
    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException
    ) {
        response.contentType = "application/json"
        response.characterEncoding = Charsets.UTF_8.name()
        response.status = HttpServletResponse.SC_UNAUTHORIZED
        response.writer.write(JsonUtil.toJson(RestBean.failure(401, "Not logged in. Please log in first.")))
    }
}
