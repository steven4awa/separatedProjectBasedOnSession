package com.ferryside.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

/**
@ Author: ferry
@ Date 05/07/2026 01:57
 */
@Configuration
class CorsConfig {

    @Bean
    @Primary
    fun corsConfigurationSource(): CorsConfigurationSource {
        val cors = CorsConfiguration().apply {
            addAllowedOriginPattern("*") // 允许所有来源
            allowCredentials = true       // 允许携带 Cookie
            addAllowedHeader("*")        // 允许所有请求头
            addAllowedMethod("*")        // 允许所有请求方法 (GET, POST 等)
            addExposedHeader("*")        // 暴露所有响应头给前端读取
        }

        val source = UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", cors) // 对所有路径生效
        }

        return source
    }
}