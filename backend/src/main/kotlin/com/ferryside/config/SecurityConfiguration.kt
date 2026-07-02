package com.ferryside.config

import com.ferryside.handler.LoginFailureHandler
import com.ferryside.handler.LoginSuccessHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.web.SecurityFilterChain


/**
@ Author: ferry
@ Date 27/06/2026 22:49
 */
@Configuration
@EnableWebSecurity
class SecurityConfiguration {

    @Bean
    fun securityFilterChain(http: HttpSecurity, successHandler: LoginSuccessHandler, failureHandler: LoginFailureHandler): SecurityFilterChain {
        http {
            authorizeHttpRequests { // 配置哪些请求需要登录，哪些不用登录。
                // 1. 优先配置：允许所有人访问登录接口
                authorize("/api/auth/login", permitAll) // `/api/auth/login` is accessible for everyone
                authorize(anyRequest, authenticated) // 除了 `/api/auth/login` 都需要认证
            }

            formLogin {
                loginProcessingUrl = "/api/auth/login" // 更改默认登录接口 只负责处理登录请求
                authenticationSuccessHandler = successHandler
                authenticationFailureHandler = failureHandler

//                permitAll()
            }
            httpBasic { }

            logout {
                logoutUrl = "/api/auth/logout"
            }

            csrf {
                disable()
            }
        }
        return http.build()
    }
}