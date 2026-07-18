package com.ferryside.controller

import com.ferryside.entity.RestBean
import com.ferryside.service.AuthorizeService
import com.ferryside.util.LogFactory
import org.hibernate.validator.constraints.Length
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
private const val USERNAME_REGEX : String = "^[A-Za-z\\u4e00-\\u9fa5]+$"

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
        val msg = service.sendValidatedEmail(email)
        return if (msg == "true")
            RestBean.success("sent email successfully")
        else
            RestBean.failure(HttpStatus.BAD_REQUEST.ordinal, msg)
    }

    @PostMapping("/register")
    fun register(@Pattern(USERNAME_REGEX)@Length(min = 2, max = 8)@RequestParam("username") username: String,
                 @RequestParam("password")@Length(min = 6, max = 16) password: String,
                 @RequestParam("email") email: String,
                 @RequestParam("code")@Length(min = 6, max = 6) code: String,
                 ): RestBean<String>{
        val msg = service.validateAndRegister(username, password, email, code)
        return if(msg == "注册成功"){
            RestBean.success(msg)
        } else{
            RestBean.failure(HttpStatus.BAD_REQUEST.ordinal, msg)
        }
    }
}