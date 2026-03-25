package com.weeth.domain.user.application.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class SocialLoginResponse(
    @field:Schema(description = "액세스 토큰")
    val accessToken: String,
    @field:Schema(description = "리프레시 토큰")
    val refreshToken: String,
    @field:Schema(description = "약관 동의 완료 여부 (true: 약관 동의 완료, false: 약관 동의 필요)", example = "true")
    val registered: Boolean,
)
