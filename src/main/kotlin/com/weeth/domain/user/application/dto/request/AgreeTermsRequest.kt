package com.weeth.domain.user.application.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.AssertTrue

data class AgreeTermsRequest(
    @field:Schema(description = "서비스 이용약관 동의", example = "true")
    @field:AssertTrue(message = "서비스 이용약관에 동의해야 합니다")
    val termsAgreed: Boolean,
    @field:Schema(description = "개인정보 처리방침 동의", example = "true")
    @field:AssertTrue(message = "개인정보 처리방침에 동의해야 합니다")
    val privacyAgreed: Boolean,
)
