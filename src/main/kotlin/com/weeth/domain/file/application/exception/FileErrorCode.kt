package com.weeth.domain.file.application.exception

import com.weeth.global.common.exception.ErrorCodeInterface
import com.weeth.global.common.exception.ExplainError
import org.springframework.http.HttpStatus

enum class FileErrorCode(
    private val code: Int,
    private val status: HttpStatus,
    private val message: String,
) : ErrorCodeInterface {
    @ExplainError("파일 ID로 조회했으나 해당 파일이 존재하지 않을 때 발생합니다.")
    FILE_NOT_FOUND(2500, HttpStatus.NOT_FOUND, "존재하지 않는 파일입니다."),

    @ExplainError("Presigned URL 생성 중 S3 연결 오류가 발생했을 때 발생합니다.")
    PRESIGNED_URL_GENERATION_FAILED(2501, HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드 URL 생성에 실패했습니다."),

    @ExplainError("허용되지 않은 Content-Type으로 파일 업로드를 시도했을 때 발생합니다.")
    UNSUPPORTED_CONTENT_TYPE(2502, HttpStatus.BAD_REQUEST, "지원하지 않는 파일 형식입니다."),

    @ExplainError("허용되지 않은 확장자로 파일 업로드를 시도했을 때 발생합니다.")
    UNSUPPORTED_FILE_EXTENSION(2503, HttpStatus.BAD_REQUEST, "지원하지 않는 파일 확장자입니다."),
    ;

    override fun getCode(): Int = code

    override fun getStatus(): HttpStatus = status

    override fun getMessage(): String = message
}
