package com.ferryside.backend

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

@SpringBootTest
class BackendApplicationTests {

    @Test
    fun contextLoads() {
        println("encoded password:")
        println(BCryptPasswordEncoder().encode("1234"))
    }

}
