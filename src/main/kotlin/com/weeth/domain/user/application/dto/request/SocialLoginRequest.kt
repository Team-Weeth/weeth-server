package com.weeth.domain.user.application.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

data class SocialLoginRequest(
    @field:Schema(description = "OAuth2 인가 코드(auth code)", example = "SplxlOBeZQQYbYS6WxSbIA")
    @field:NotBlank
    val authCode: String,
)
