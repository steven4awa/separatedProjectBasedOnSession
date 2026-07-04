package com.ferryside.backend

import com.ferryside.entity.Users
import com.ferryside.mapper.UsersMapper
import com.ferryside.service.UserService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

@SpringBootTest
class BackendApplicationTests(
    @Autowired
    private val usersMapper: UsersMapper,
) {
    @Autowired
    lateinit var userService: UserService


    @Test
    fun contextLoads() {
        println("encoded password:")
        println(BCryptPasswordEncoder().encode("1234"))

        val userList: MutableList<Users?>? = usersMapper.selectList(null)
        userList!!.forEach {
            println(it)
        }
    }

    @Test
    fun testFindByUsername() {
        val user = userService.findAccountByUsernameOrEmail("admin")

        println(user)

        assertNotNull(user)
        assertEquals("admin", user?.username)
    }

}
