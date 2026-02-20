package com.weeth.global.auth.apple.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class AppleTokenResponse(
    @field:JsonProperty("access_token")
    val accessToken: String,
    @field:JsonProperty("token_type")
    val tokenType: String,
    @field:JsonProperty("expires_in")
    val expiresIn: Long,
    @field:JsonProperty("refresh_token")
    val refreshToken: String,
    @field:JsonProperty("id_token")
    val idToken: String,
)
