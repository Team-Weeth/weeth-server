package com.weeth.domain.account.application.dto.response

import com.weeth.domain.account.domain.enums.AccountTransactionDirection
import com.weeth.domain.account.domain.enums.AccountTransactionType
import com.weeth.domain.file.application.dto.response.FileResponse
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
    @field:Schema(
        description = "해당 거래 직후의 실제 통장 잔액 (원)",
        example = "1520000",
    )
    val balanceAfter: Int,
    @field:Schema(description = "메모", nullable = true)
    val memo: String?,
    @field:Schema(description = "영수증 존재 여부", example = "true")
    val hasReceipt: Boolean = false,
    @field:Schema(description = "영수증 파일 목록. 목록 조회에서는 빈 배열로 반환될 수 있음")
    val receipts: List<FileResponse> = emptyList(),
)
