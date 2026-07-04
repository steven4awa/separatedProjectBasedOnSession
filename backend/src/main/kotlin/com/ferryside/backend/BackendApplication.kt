package com.ferryside.backend

import org.mybatis.spring.annotation.MapperScan
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication
@ComponentScan("com.ferryside")
@MapperScan("com.ferryside.mapper")
class BackendApplication

fun main(args: Array<String>) {
    runApplication<BackendApplication>(*args)
}
