package com.weeth.domain.account.application.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class MonthlyBalanceResponse(
    @field:Schema(description = "집계 월 (yyyy-MM)", example = "2026-07")
    val yearMonth: String,
    @field:Schema(description = "해당 월 수입 합계 (원)", example = "240000")
    val income: Int,
    @field:Schema(description = "해당 월 지출 합계 (원)", example = "50000")
    val expense: Int,
    @field:Schema(description = "해당 월 말 누적 잔액 (원)", example = "190000")
    val endingBalance: Int,
)
