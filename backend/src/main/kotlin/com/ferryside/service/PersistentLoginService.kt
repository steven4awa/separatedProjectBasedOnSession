package com.ferryside.service

import com.baomidou.mybatisplus.extension.service.IService
import com.ferryside.entity.PersistentLogin
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository

/**
@ Author: ferry
@ Date 06/07/2026 13:14
 */
interface PersistentLoginService : IService<PersistentLogin> , PersistentTokenRepository{
}