package com.weeth.domain.account.application.dto.response

import com.weeth.global.common.response.SliceResponse
import io.swagger.v3.oas.annotations.media.Schema

data class MemberAccountTransactionsResponse(
    @field:Schema(description = "필터 탭별 부원 공개 거래 건수 요약. 첫 페이지(page=0)에서만 제공", nullable = true)
    val counts: TransactionCountsResponse?,
    @field:Schema(description = "회비 집계 행. 첫 페이지(page=0)에서만 제공", nullable = true)
    val duesSummary: DuesSummaryResponse?,
    @field:Schema(description = "부원 공개 거래 내역 (무한 스크롤)")
    val transactions: SliceResponse<MemberTransactionResponse>,
) {
    data class TransactionCountsResponse(
        @field:Schema(description = "전체 공개 거래 수", example = "4")
        val all: Int,
        @field:Schema(description = "지출 공개 거래 수", example = "3")
        val expense: Int,
        @field:Schema(description = "수입 공개 거래 수", example = "1")
        val income: Int,
        @field:Schema(description = "회비 집계 행 수. 집계 금액이 있으면 1", example = "1")
        val dues: Int,
    )

    data class DuesSummaryResponse(
        @field:Schema(description = "회비 집계 행 라벨", example = "회비")
        val label: String = "회비", // 고정 레이블
        @field:Schema(description = "회비 납부 거래 합계", example = "1100000")
        val totalAmount: Int,
        @field:Schema(description = "회비 집계 행 설명", example = "납부될 때마다 합산돼요")
        val description: String = "납부될 때마다 합산돼요", // 고정 설명
    )
}
