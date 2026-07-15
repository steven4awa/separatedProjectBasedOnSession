package com.ferryside.handler

import com.ferryside.entity.RestBean
import com.ferryside.util.JsonUtil
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component

/**
@ Author: ferry
@ Date 15/07/2026
 */
@Component
class ForbiddenHandler : AccessDeniedHandler {
    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessDeniedException: AccessDeniedException
    ) {
        response.contentType = "application/json"
        response.characterEncoding = Charsets.UTF_8.name()
        response.status = HttpServletResponse.SC_FORBIDDEN
        response.writer.write(JsonUtil.toJson(RestBean.failure(403, "Access denied.")))
    }
}
