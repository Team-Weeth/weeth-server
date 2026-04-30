package com.weeth.global.auth.jwt.domain.enums

enum class TokenType {
    TEMPORARY, // 약관 미동의 사용자용 (약관 동의 API만 접근 가능)
    ACCESS, // 정상 사용자용
}
