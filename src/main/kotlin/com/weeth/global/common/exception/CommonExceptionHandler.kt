package com.weeth.global.common.exception

import com.weeth.global.common.response.CommonResponse
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.validation.BindException
import org.springframework.web.ErrorResponse
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@RestControllerAdvice
class CommonExceptionHandler {
    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(BaseException::class)
    fun handle(ex: BaseException): ResponseEntity<CommonResponse<Void?>> {
        log.warn("구체로그: ", ex)
        log.warn(LOG_FORMAT, ex::class.simpleName, ex.statusCode, ex.message)

        val errorCode = ex.errorCode
        val response: CommonResponse<Void?> =
            if (errorCode != null) {
                CommonResponse.error(errorCode)
            } else {
                CommonResponse.createFailure(ex.statusCode, ex.message ?: "")
            }

        return ResponseEntity
            .status(ex.statusCode)
            .body(response)
    }

    @ExceptionHandler(BindException::class)
    fun handle(ex: BindException): ResponseEntity<CommonResponse<List<BindExceptionResponse>>> {
        var statusCode = 400
        val exceptionResponses = mutableListOf<BindExceptionResponse>()

        if (ex is ErrorResponse) {
            statusCode = ex.statusCode.value()
            ex.bindingResult.fieldErrors.forEach { fieldError ->
                exceptionResponses.add(
                    BindExceptionResponse(
                        message = fieldError.defaultMessage,
                        value = fieldError.rejectedValue,
                    ),
                )
            }
        }

        log.warn("구체로그: ", ex)
        log.warn(LOG_FORMAT, ex::class.simpleName, statusCode, exceptionResponses)

        val response = CommonResponse.createFailure(statusCode, "bindException", exceptionResponses.toList())

        return ResponseEntity
            .status(statusCode)
            .body(response)
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handle(ex: MethodArgumentTypeMismatchException): ResponseEntity<CommonResponse<Void?>> {
        var statusCode = 400
        if (ex is ErrorResponse) {
            statusCode = ex.statusCode.value()
        }

        log.warn("구체로그: ", ex)
        log.warn(LOG_FORMAT, ex::class.simpleName, statusCode, ex.message)

        val response = CommonResponse.createFailure(statusCode, INPUT_FORMAT_ERROR_MESSAGE)

        return ResponseEntity
            .status(statusCode)
            .body(response)
    }

    @ExceptionHandler(Exception::class)
    fun handle(ex: Exception): ResponseEntity<CommonResponse<Void?>> {
        var statusCode = 500

        if (ex is ErrorResponse) {
            statusCode = ex.statusCode.value()
        }

        log.warn("구체로그: ", ex)
        log.warn(LOG_FORMAT, ex::class.simpleName, statusCode, ex.message)

        val response = CommonResponse.createFailure(statusCode, ex.message ?: "")

        return ResponseEntity
            .status(statusCode)
            .body(response)
    }

    companion object {
        private const val INPUT_FORMAT_ERROR_MESSAGE = "입력 포맷이 올바르지 않습니다."
        private const val LOG_FORMAT = "Class : {}, Code : {}, Message : {}"
    }
}
