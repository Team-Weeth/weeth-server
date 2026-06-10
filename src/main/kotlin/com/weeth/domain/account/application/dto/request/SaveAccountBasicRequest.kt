package com.weeth.domain.account.application.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

data class SaveAccountBasicRequest(
    @field:Schema(description = "회비 이름", example = "5기 정기 회비")
    @field:Size(max = 30)
    @field:NotBlank
    val name: String,
    @field:Schema(description = "1인 회비 금액 (원)", example = "50000")
    @field:Positive
    val duesAmount: Int,
    @field:Schema(description = "회비 설명", example = "동아리 운영비로 사용됩니다.", nullable = true)
    @field:Size(max = 30)
    val description: String?,
)
