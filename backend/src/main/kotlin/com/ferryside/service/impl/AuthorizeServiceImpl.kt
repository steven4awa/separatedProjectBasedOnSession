package com.ferryside.service.impl

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import com.ferryside.entity.Users
import com.ferryside.enums.EmailType
import com.ferryside.mapper.UsersMapper
import com.ferryside.service.AuthorizeService
import com.ferryside.service.UserService
import jakarta.servlet.http.HttpSession
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.mail.MailSender
import org.springframework.mail.SimpleMailMessage
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
@ Author: ferry
@ Date 10/07/2026 16:10
 */
@Service
class AuthorizeServiceImpl(
    private val userService: UserService,
    private val mailSender: MailSender,
    @Value($$"${spring.mail.username}")
    private val sendMail: String,
    private val template: StringRedisTemplate,
    private val session: HttpSession,
) : AuthorizeService, ServiceImpl<UsersMapper, Users>() {

    override fun loadUserByUsername(username: String): UserDetails {
        val user = userService.findAccountByUsernameOrEmail(username)
            ?: throw UsernameNotFoundException("User not found")

        return User(
            user.username,
            user.password, // 必须是 String
            listOf() // 先不给权限，后面可以加 ROLE_USER
        )
    }


    override fun validateAndRegister(
        username: String,
        password: String,
        email: String,
        code: String
    ): String {
        val msg = validateKeyInRedis(email, code, EmailType.REGISTER)
        if(msg == "RIGHT-CODE"){
            save(Users(username = username, password = password, email = email))
            return msg
        }
        return msg
    }

private fun validateKeyInRedis(email: String, code: String, type: EmailType): String {
        val key = getEmailKey(email, type)
        if(template.hasKey(key)){
            val codeValue = template.opsForValue().get(key) as String
            if(codeValue == ""){
                return "验证码失效"
            }
            return if(codeValue == code){
                "RIGHT-CODE"
            } else{
                "验证码错误"
            }

        } else
        return "请先发送验证码到邮箱"
    }

    override fun varifyOnly(
        email: String,
        code: String,
    ): String {
        val msg = validateKeyInRedis(
            email,
            code,
            EmailType.RESET_PASSWORD
        )

        if (msg != "RIGHT-CODE")
            return msg

        return "true"
    }


    private fun getEmailKey(email: String, type: EmailType): String {
        return "${type.name}:${session.id}:$email"
    }

    /**
     * 1. 生成验证码
     * 2. 把邮箱和对应的验证码存入 Redis（过期时间3分钟）
     * 3. 发送验证码到指定邮箱
     * 4. 如果发送失败，把 Redis里面的刚刚插入的内容删除
     * 5. 用户在注册时，再从 Redis里面取出对应键值对，查看是否一致
     */
    override fun sendValidatedEmail(email: String, type: EmailType): String {
        val key = getEmailKey(email, type)
        if(template.hasKey(key)){
            val expire = template.getExpire(key, TimeUnit.SECONDS)
            if(expire > 120)
                return "false" // 请求频繁
        }
        if(type == EmailType.REGISTER && userService.findAccountByUsernameOrEmail(email) != null){
            return "该邮箱已被注册"
        }
        if(type == EmailType.RESET_PASSWORD &&userService.findAccountByUsernameOrEmail(email) == null){
            return "该邮箱还未注册"
        }

        val randomCode = Random.nextInt(100000,1000000)
        val message = SimpleMailMessage().apply {
            from = sendMail
            setTo(email)
            subject = "Verification Code"
            text = "Your varication code is $randomCode"
        }
        //  Operations for value
        template.opsForValue().set(key, randomCode.toString(), Duration.ofMinutes(3))
        try{
            mailSender.send(message)
        } catch (e: Exception){
            template.delete(key)
            e.printStackTrace()
            return "false"
        }

        return "true"
    }
}