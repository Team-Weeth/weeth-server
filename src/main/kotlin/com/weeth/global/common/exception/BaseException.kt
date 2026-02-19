package com.weeth.global.common.exception

abstract class BaseException : RuntimeException {
    val statusCode: Int
    val errorCode: ErrorCodeInterface?

    constructor(code: Int, message: String) : super(message) {
        statusCode = code
        errorCode = null
    }

    constructor(code: Int, message: String, cause: Throwable) : super(message, cause) {
        statusCode = code
        errorCode = null
    }

    constructor(errorCode: ErrorCodeInterface) : super(errorCode.getMessage()) {
        statusCode = errorCode.getStatus().value()
        this.errorCode = errorCode
    }

    constructor(errorCode: ErrorCodeInterface, cause: Throwable?) : super(errorCode.getMessage(), cause) {
        statusCode = errorCode.getStatus().value()
        this.errorCode = errorCode
    }
}
