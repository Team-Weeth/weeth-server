package com.weeth.domain.user.application.dto.request

import io.swagger.v3.oas.annotations.media.Schema

data class AgreeTermsRequest(
    @field:Schema(description = "서비스 이용약관 동의", example = "true")
    val termsAgreed: Boolean,
    @field:Schema(description = "개인정보 처리방침 동의", example = "true")
    val privacyAgreed: Boolean,
)
