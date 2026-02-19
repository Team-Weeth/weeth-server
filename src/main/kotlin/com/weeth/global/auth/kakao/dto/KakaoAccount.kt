package com.weeth.global.auth.kakao.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class KakaoAccount(
    @field:JsonProperty("is_email_valid")
    val isEmailValid: Boolean,
    @field:JsonProperty("is_email_verified")
    val isEmailVerified: Boolean,
    @field:JsonProperty("email")
    val email: String,
)
