package com.weeth.global.auth.kakao.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class KakaoProfile(
    @field:JsonProperty("nickname")
    val nickname: String?,
)
