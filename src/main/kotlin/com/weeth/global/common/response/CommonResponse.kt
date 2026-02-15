package com.weeth.global.common.response

import com.weeth.global.common.exception.ErrorCodeInterface

data class CommonResponse<T>(
    val code: Int,
    val message: String,
    val data: T?,
) {
    companion object {
        private const val DEFAULT_SUCCESS_CODE = 200

        @JvmStatic
        fun <T> success(responseCode: ResponseCodeInterface): CommonResponse<T> =
            CommonResponse(
                code = responseCode.code,
                message = responseCode.message,
                data = null,
            )

        @JvmStatic
        fun <T> success(
            responseCode: ResponseCodeInterface,
            data: T,
        ): CommonResponse<T> =
            CommonResponse(
                code = responseCode.code,
                message = responseCode.message,
                data = data,
            )

        @JvmStatic
        fun <T> success(message: String): CommonResponse<T> =
            CommonResponse(
                code = DEFAULT_SUCCESS_CODE,
                message = message,
                data = null,
            )

        @JvmStatic
        fun <T> success(message: String, data: T): CommonResponse<T> =
            CommonResponse(
                code = DEFAULT_SUCCESS_CODE,
                message = message,
                data = data,
            )

        @JvmStatic
        fun <T> createSuccess(message: String): CommonResponse<T> = success(message)

        @JvmStatic
        fun <T> createSuccess(message: String, data: T): CommonResponse<T> = success(message, data)

        @JvmStatic
        fun error(errorCode: ErrorCodeInterface): CommonResponse<Void?> =
            CommonResponse(
                code = errorCode.code,
                message = errorCode.message,
                data = null,
            )

        @JvmStatic
        fun error(
            errorCode: ErrorCodeInterface,
            message: String,
        ): CommonResponse<Void?> =
            CommonResponse(
                code = errorCode.code,
                message = message,
                data = null,
            )

        @JvmStatic
        fun <T> error(
            errorCode: ErrorCodeInterface,
            data: T,
        ): CommonResponse<T> =
            CommonResponse(
                code = errorCode.code,
                message = errorCode.message,
                data = data,
            )

        @JvmStatic
        fun createFailure(code: Int, message: String): CommonResponse<Void?> =
            CommonResponse(
                code = code,
                message = message,
                data = null,
            )

        @JvmStatic
        fun <T> createFailure(code: Int, message: String, data: T): CommonResponse<T> =
            CommonResponse(
                code = code,
                message = message,
                data = data,
            )
    }
}
