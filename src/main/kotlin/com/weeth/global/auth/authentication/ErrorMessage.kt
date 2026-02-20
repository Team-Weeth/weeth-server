package com.weeth.global.auth.authentication

enum class ErrorMessage(
    val code: Int,
    val message: String,
) {
    UNAUTHORIZED(401, "인증 정보가 존재하지 않습니다."),
    FORBIDDEN(403, "권한이 없습니다."),
}
