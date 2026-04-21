package com.weeth.global.common.exception

import com.weeth.global.common.response.CommonResponse
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.http.ResponseEntity
import org.springframework.validation.BindException
import org.springframework.web.ErrorResponse
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@RestControllerAdvice
class CommonExceptionHandler {
    private val errorLog = LoggerFactory.getLogger("ERROR_LOG")

    @ExceptionHandler(BaseException::class)
    fun handle(ex: BaseException): ResponseEntity<CommonResponse<*>> {
        logException(ex.statusCode, ex, ex.message)

        val response =
            if (ex.data != null) {
                CommonResponse.error(ex.errorCode, ex.data)
            } else {
                CommonResponse.error(ex.errorCode)
            }

        return ResponseEntity
            .status(ex.statusCode)
            .body(response)
    }

    @ExceptionHandler(BindException::class)
    fun handle(ex: BindException): ResponseEntity<CommonResponse<List<BindExceptionResponse>>> {
        val statusCode = if (ex is ErrorResponse) ex.statusCode.value() else 400
        val exceptionResponses = mutableListOf<BindExceptionResponse>()

        if (ex is ErrorResponse) {
            ex.bindingResult.fieldErrors.forEach { fieldError ->
                exceptionResponses.add(
                    BindExceptionResponse(
                        message = fieldError.defaultMessage,
                        value = fieldError.rejectedValue,
                    ),
                )
            }
        }

        logException(statusCode, ex, exceptionResponses)

        val response = CommonResponse.createFailure(statusCode, "bindException", exceptionResponses.toList())

        return ResponseEntity
            .status(statusCode)
            .body(response)
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handle(ex: MethodArgumentTypeMismatchException): ResponseEntity<CommonResponse<Void?>> {
        val statusCode = if (ex is ErrorResponse) ex.statusCode.value() else 400

        logException(statusCode, ex, ex.message)

        val response = CommonResponse.createFailure(statusCode, INPUT_FORMAT_ERROR_MESSAGE)

        return ResponseEntity
            .status(statusCode)
            .body(response)
    }

    @ExceptionHandler(Exception::class)
    fun handle(ex: Exception): ResponseEntity<CommonResponse<Void?>> {
        val statusCode = if (ex is ErrorResponse) ex.statusCode.value() else 500

        logException(statusCode, ex, ex.message, error = true)

        val response = CommonResponse.createFailure(statusCode, ex.message ?: "")

        return ResponseEntity
            .status(statusCode)
            .body(response)
    }

    companion object {
        private const val INPUT_FORMAT_ERROR_MESSAGE = "입력 포맷이 올바르지 않습니다."
        private const val LOG_FORMAT = "Class : {}, Code : {}, Message : {}"
    }

    private fun logException(
        statusCode: Int,
        ex: Throwable,
        message: Any?,
        error: Boolean = false,
    ) {
        try {
            MDC.put("status", statusCode.toString())
            MDC.put("errorType", ex::class.simpleName ?: "Exception")
            MDC.put("errorMessage", ex.message ?: message?.toString().orEmpty())
            if (error) {
                errorLog.error(LOG_FORMAT, ex::class.simpleName, statusCode, message, ex)
            } else {
                errorLog.warn(LOG_FORMAT, ex::class.simpleName, statusCode, message)
            }
        } finally {
            MDC.remove("status")
            MDC.remove("errorType")
            MDC.remove("errorMessage")
        }
    }
}
