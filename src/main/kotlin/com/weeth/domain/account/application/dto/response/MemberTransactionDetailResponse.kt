package com.weeth.domain.account.application.dto.response

import com.weeth.domain.account.domain.enums.AccountTransactionDirection
import com.weeth.domain.account.domain.enums.AccountTransactionType
import com.weeth.domain.file.application.dto.response.FileResponse
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class MemberTransactionDetailResponse(
    @field:Schema(description = "거래 ID", example = "1")
    val transactionId: Long,
    @field:Schema(description = "거래 유형", example = "EXPENSE")
    val type: AccountTransactionType,
    @field:Schema(description = "거래 방향", example = "EXPENSE")
    val direction: AccountTransactionDirection,
    @field:Schema(description = "거래 내용", example = "스터디 지원금")
    val title: String,
    @field:Schema(description = "거래처. REFUND는 환불로 마스킹", example = "인프런", nullable = true)
    val source: String?,
    @field:Schema(description = "거래 금액", example = "50000")
    val amount: Int,
    @field:Schema(description = "거래 일시", example = "2026-07-20T00:00:00")
    val transactedAt: LocalDateTime,
    @field:Schema(description = "거래 분류", example = "운영비", nullable = true)
    val category: String?,
    @field:Schema(description = "등록자 이름 스냅샷", example = "운영진 김검도")
    val registeredByName: String,
    @field:Schema(description = "메모", nullable = true)
    val memo: String?,
    @field:Schema(description = "영수증 존재 여부", example = "true")
    val hasReceipt: Boolean = false,
    @field:Schema(description = "영수증 파일 목록. 최대 1개")
    val receipts: List<FileResponse> = emptyList(),
)
