package com.weeth.global.common.response

import org.springframework.http.HttpStatus

interface ResponseCodeInterface {
    val code: Int
    val status: HttpStatus
    val message: String
}
