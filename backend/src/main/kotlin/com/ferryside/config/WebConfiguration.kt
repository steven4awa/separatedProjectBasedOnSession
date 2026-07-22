package com.ferryside.config

import com.ferryside.interceptor.AuthorizeInterceptor
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
@ Author: ferry
@ Date 22/07/2026 06:31
 */
@Configuration
class WebConfiguration(
    private val authorizeInterceptor: AuthorizeInterceptor
): WebMvcConfigurer {
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(authorizeInterceptor).addPathPatterns("/**").excludeHttpMethods().addPathPatterns("/api/auth/**")
    }
}