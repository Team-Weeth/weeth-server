package com.weeth.global.common.exception

import org.springframework.http.HttpStatus

interface ErrorCodeInterface {
    val code: Int
    val status: HttpStatus
    val message: String

    @Throws(NoSuchFieldException::class)
    fun getExplainError(): String {
        val field = this::class.java.getField((this as Enum<*>).name)
        val annotation = field.getAnnotation(ExplainError::class.java)
        return annotation?.value ?: message
    }
}
