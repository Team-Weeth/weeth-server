package com.weeth.global.common.exception

import org.springframework.http.HttpStatus

interface ErrorCodeInterface {
    fun getCode(): Int

    fun getStatus(): HttpStatus

    fun getMessage(): String

    @Throws(NoSuchFieldException::class)
    fun getExplainError(): String {
        val field = this::class.java.getField((this as Enum<*>).name)
        val annotation = field.getAnnotation(ExplainError::class.java)
        return annotation?.value ?: getMessage()
    }
}
