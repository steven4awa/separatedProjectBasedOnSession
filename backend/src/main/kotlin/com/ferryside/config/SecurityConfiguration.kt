package com.ferryside.config

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
    fun securityFilterChain(http: HttpSecurity, successHandler: LoginSuccessHandler): SecurityFilterChain {
        http {
            authorizeHttpRequests { // 配置哪些请求需要登录，哪些不用登录。
                authorize(anyRequest, authenticated)
            }

            formLogin {
                loginProcessingUrl = "/api/auth/login"
                authenticationSuccessHandler = successHandler
            }


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