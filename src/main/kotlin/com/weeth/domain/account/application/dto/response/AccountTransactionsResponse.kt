package com.weeth.domain.account.application.dto.response

import com.weeth.global.common.response.PageResponse
import io.swagger.v3.oas.annotations.media.Schema

data class AccountTransactionsResponse(
    @field:Schema(description = "필터 탭별 거래 건수 요약")
    val counts: TransactionCountsResponse,
    @field:Schema(description = "거래 내역 페이지")
    val transactions: PageResponse<AccountTransactionResponse>,
) {
    data class TransactionCountsResponse(
        @field:Schema(description = "전체 거래 수", example = "18")
        val all: Int,
        @field:Schema(description = "지출 거래 수", example = "10")
        val expense: Int,
        @field:Schema(description = "수입 거래 수", example = "2")
        val income: Int,
        @field:Schema(description = "회비 거래 수", example = "3")
        val dues: Int,
    )
}
