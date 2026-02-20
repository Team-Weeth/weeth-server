package com.weeth.domain.account.application.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

data class AccountSaveRequest(
    @field:Schema(description = "회비 설명", example = "2024년 2학기 회비")
    @field:NotBlank
    val description: String,
    @field:Schema(description = "총 금액", example = "100000")
    @field:NotNull
    @field:Positive
    val totalAmount: Int,
    @field:Schema(description = "기수", example = "4")
    @field:NotNull
    val cardinal: Int,
)
