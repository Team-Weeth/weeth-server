package com.weeth.domain.account.application.dto.response

import com.weeth.domain.account.domain.enums.AccountTransactionDirection
import com.weeth.domain.account.domain.enums.AccountTransactionType
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class AccountTransactionResponse(
    @field:Schema(description = "거래 ID", example = "1")
    val transactionId: Long,
    @field:Schema(description = "거래 유형", example = "EXPENSE")
    val type: AccountTransactionType,
    @field:Schema(description = "거래 방향 (INCOME: 수입, EXPENSE: 지출)", example = "EXPENSE")
    val direction: AccountTransactionDirection,
    @field:Schema(description = "거래 내용", example = "스터디 지원금")
    val title: String,
    @field:Schema(description = "거래처", example = "인프런")
    val source: String?,
    @field:Schema(description = "거래 금액 (원, 부호 없는 양수). 부호는 direction 으로 표기", example = "50000")
    val amount: Int,
    @field:Schema(description = "거래 일시", example = "2026-07-20T00:00:00")
    val transactedAt: LocalDateTime,
    @field:Schema(description = "메모", nullable = true)
    val memo: String?,
    // TODO: 영수증 추가
)
