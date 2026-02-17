package com.weeth.domain.file.presentation

import com.weeth.global.common.response.ResponseCodeInterface
import org.springframework.http.HttpStatus

enum class FileResponseCode(
    override val code: Int,
    override val status: HttpStatus,
    override val message: String,
) : ResponseCodeInterface {
    PRESIGNED_URL_GET_SUCCESS(1500, HttpStatus.OK, "Presigned Url 반환에 성공했습니다"),
}
