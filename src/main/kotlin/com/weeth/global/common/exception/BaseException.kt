package com.weeth.global.common.exception

abstract class BaseException(
    val errorCode: ErrorCodeInterface,
    message: String? = null,
    val data: Any? = null,
) : RuntimeException(message ?: errorCode.message) {
    val statusCode: Int get() = errorCode.status.value()
}
