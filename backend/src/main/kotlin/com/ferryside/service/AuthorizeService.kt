package com.ferryside.service

import org.springframework.security.core.userdetails.UserDetailsService

/**
@ Author: ferry
@ Date 29/06/2026 19:31
 */

interface AuthorizeService : UserDetailsService{
    fun sendValidatedEmail(email: String): Boolean
}