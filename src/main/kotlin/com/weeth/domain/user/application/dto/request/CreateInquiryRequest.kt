package com.weeth.domain.user.application.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateInquiryRequest(
    @field:Schema(description = "이메일", example = "user@example.com")
    @field:NotBlank
    @field:Email
    @field:Size(max = 255)
    val email: String,
    @field:Schema(description = "문의 내용", example = "서비스에 대해 문의드립니다.")
    @field:Size(max = 1000)
    val message: String?,
)
