package com.weeth.domain.account.application.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

data class MarkPaymentPaidRequest(
    @field:Schema(description = "납부 완료 처리할 납부 대상 ID 목록 (단건은 길이 1)", example = "[1, 2, 3]")
    @field:NotEmpty
    val targetIds: List<Long>,
    @field:Schema(description = "납부 일시. null이면 현재 시각으로 처리", example = "2026-07-20T14:00:00", nullable = true)
    val paidAt: LocalDateTime? = null,
    @field:Schema(description = "납부 거래에 남길 메모", nullable = true)
    @field:Size(max = 200)
    val memo: String? = null,
)
