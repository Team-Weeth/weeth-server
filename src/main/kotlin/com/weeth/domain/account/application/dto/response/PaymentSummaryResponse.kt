package com.weeth.domain.account.application.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class PaymentSummaryResponse(
    @field:Schema(description = "납부 완료 인원", example = "3")
    val paidCount: Int,
    @field:Schema(description = "전체 납부 대상 인원", example = "24")
    val totalTargetCount: Int,
)
