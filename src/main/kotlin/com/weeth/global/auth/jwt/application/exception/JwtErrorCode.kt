package com.weeth.global.auth.jwt.application.exception

import com.weeth.global.common.exception.ErrorCodeInterface
import com.weeth.global.common.exception.ExplainError
import org.springframework.http.HttpStatus

enum class JwtErrorCode(
    private val code: Int,
    private val status: HttpStatus,
    private val message: String,
) : ErrorCodeInterface {
    @ExplainError("토큰의 구조가 올바르지 않거나(Malformed), 서명이 유효하지 않은 경우 발생합니다. 토큰을 재발급 받아주세요.")
    INVALID_TOKEN(2900, HttpStatus.BAD_REQUEST, "올바르지 않은 Token 입니다."),

    @ExplainError("Redis에 해당 리프레시 토큰이 존재하지 않습니다. 토큰이 만료되었거나, 이미 로그아웃(삭제)된 상태일 수 있습니다. 다시 로그인해주세요.")
    REDIS_TOKEN_NOT_FOUND(2901, HttpStatus.NOT_FOUND, "저장된 리프레시 토큰이 존재하지 않습니다."),

    @ExplainError("API 요청 헤더(Authorization)에 토큰 값이 포함되지 않았거나 비어있을 때 발생합니다.")
    TOKEN_NOT_FOUND(2902, HttpStatus.NOT_FOUND, "헤더에서 토큰을 찾을 수 없습니다."),

    @ExplainError("인증이 필요한 리소스에 인증 정보 없이(Anonymous) 접근을 시도했을 때 발생합니다. (Spring Security 필터 단계 차단)")
    ANONYMOUS_AUTHENTICATION(2903, HttpStatus.UNAUTHORIZED, "인증정보가 존재하지 않습니다."),

    @ExplainError("Apple 인증 과정에서 토큰 교환 또는 검증에 실패했을 때 발생합니다.")
    APPLE_AUTHENTICATION_FAILED(2904, HttpStatus.UNAUTHORIZED, "애플 로그인에 실패했습니다."),
    ;

    override fun getCode(): Int = code

    override fun getStatus(): HttpStatus = status

    override fun getMessage(): String = message
}
