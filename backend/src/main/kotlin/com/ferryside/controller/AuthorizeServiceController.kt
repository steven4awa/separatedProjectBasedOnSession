package com.ferryside.controller

import com.ferryside.entity.RestBean
import com.ferryside.service.AuthorizeService
import com.ferryside.util.LogFactory
import lombok.extern.java.Log
import org.intellij.lang.annotations.Pattern
import org.springframework.http.HttpStatus
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
@ Author: ferry
@ Date 10/07/2026 16:30
 */

private const val EMAIL_REGEX : String = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"

@Validated
@RestController
@RequestMapping("/api/auth/")
class AuthorizeServiceController (
    private val service: AuthorizeService
){

    private val log = LogFactory.logger<AuthorizeServiceController>()

    @PostMapping("/valid-email")
    fun validateEmail(@Pattern(EMAIL_REGEX)@RequestParam("email") email: String): RestBean<String>{ // @RequestParam 获取请求参数
        log.info("Validating email: $email")
        return if (service.sendValidatedEmail(email))
            RestBean.success("sent email successfully")
        else
            RestBean.failure(HttpStatus.BAD_REQUEST.ordinal, "Invalid email")
    }
}