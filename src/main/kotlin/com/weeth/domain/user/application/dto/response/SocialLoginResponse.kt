package com.weeth.domain.user.application.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class SocialLoginResponse(
    @field:Schema(description = "액세스 토큰")
    val accessToken: String,
    @field:Schema(description = "리프레시 토큰")
    val refreshToken: String,
    @field:Schema(description = "신규 회원 여부", example = "true")
    val isNewUser: Boolean,
)
