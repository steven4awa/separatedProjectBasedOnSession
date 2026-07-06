package com.ferryside.service.impl

import com.baomidou.mybatisplus.extension.kotlin.KtQueryWrapper
import com.baomidou.mybatisplus.extension.kotlin.KtUpdateWrapper
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import com.ferryside.entity.PersistentLogin
import com.ferryside.mapper.PersistentLoginMapper
import com.ferryside.service.PersistentLoginService
import org.springframework.security.web.authentication.rememberme.PersistentRememberMeToken
import org.springframework.stereotype.Service
import java.time.ZoneId
import java.util.Date

/**
@ Author: ferry
@ Date 06/07/2026 13:15
 */
@Service
class PersistentLoginServiceImpl : PersistentLoginService, ServiceImpl<PersistentLoginMapper, PersistentLogin>(){
    override fun createNewToken(token: PersistentRememberMeToken) {
        save( // insert
            PersistentLogin(
                series = token.series,
                username = token.username,
                token = token.tokenValue,
                lastUsed = token.date
                    .toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime()
            )
        )
    }

    override fun updateToken(series: String, tokenValue: String, lastUsed: Date) {
        update(
            KtUpdateWrapper(PersistentLogin::class.java)
                .eq(PersistentLogin::series, series)
                .set(
                    PersistentLogin::token,
                    tokenValue
                )
                .set(
                    PersistentLogin::lastUsed,
                    lastUsed
                        .toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime()
                )
        )
    }

    override fun getTokenForSeries(seriesId: String): PersistentRememberMeToken? {
        val entity = getOne(
            KtQueryWrapper(PersistentLogin::class.java)
                .eq(PersistentLogin::series, seriesId)
        ) ?: return null

        return PersistentRememberMeToken(
            entity.username,
            entity.series,
            entity.token,
            Date.from(
                entity.lastUsed
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
            )
        )
    }

    override fun removeUserTokens(username: String) {
        remove(
            KtQueryWrapper(PersistentLogin::class.java)
                .eq(PersistentLogin::username, username)
        )
    }
}