package com.weeth.global.auth.kakao.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class KakaoAccessToken(
    @field:JsonProperty("access_token")
    val accessToken: String,
)
