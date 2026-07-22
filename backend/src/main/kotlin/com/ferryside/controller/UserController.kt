package com.ferryside.controller

import com.ferryside.entity.RestBean
import com.ferryside.entity.auth.Account
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.SessionAttribute

/**
@ Author: ferry
@ Date 22/07/2026 06:21
 */
@RestController
@RequestMapping("/api/user")
class UserController {
    @GetMapping("/me")
    fun me(@SessionAttribute("account") account: Account): RestBean<Account>{
        return RestBean.success(account)
    }
}