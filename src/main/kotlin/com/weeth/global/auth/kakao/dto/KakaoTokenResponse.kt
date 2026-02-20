package com.weeth.global.auth.kakao.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class KakaoTokenResponse(
    @field:JsonProperty("token_type")
    val tokenType: String,
    @field:JsonProperty("access_token")
    val accessToken: String,
    @field:JsonProperty("expires_in")
    val expiresIn: Int,
    @field:JsonProperty("refresh_token")
    val refreshToken: String,
    @field:JsonProperty("refresh_token_expires_in")
    val refreshTokenExpiresIn: Int,
)
