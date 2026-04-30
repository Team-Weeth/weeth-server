package com.weeth.global.auth.kakao.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class KakaoUserInfoResponse(
    @field:JsonProperty("id")
    val id: Long,
    @field:JsonProperty("kakao_account")
    val kakaoAccount: KakaoAccount,
)
