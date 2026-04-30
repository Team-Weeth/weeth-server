package com.weeth.domain.account.application.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class AccountResponse(
    @field:Schema(description = "회비 ID", example = "1")
    val accountId: Long,
    @field:Schema(description = "회비 설명", example = "2024년 2학기 회비")
    val description: String,
    @field:Schema(description = "총 금액", example = "100000")
    val totalAmount: Int,
    @field:Schema(description = "현재 금액", example = "90000")
    val currentAmount: Int,
    @field:Schema(description = "최종 수정 시각")
    val time: LocalDateTime?,
    @field:Schema(description = "기수", example = "40")
    val cardinal: Int,
    @field:Schema(description = "영수증 목록")
    val receipts: List<ReceiptResponse>,
)
