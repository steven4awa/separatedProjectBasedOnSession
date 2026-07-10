package com.ferryside.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory


/**
@ Author: ferry
@ Date 10/07/2026 21:00
 */
object LogFactory {
    inline fun <reified T> logger(): Logger {
        return LoggerFactory.getLogger(T::class.java)
    }
}