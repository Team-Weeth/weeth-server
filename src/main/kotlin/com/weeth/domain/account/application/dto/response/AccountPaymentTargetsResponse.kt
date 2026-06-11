package com.weeth.domain.account.application.dto.response

import com.weeth.global.common.response.PageResponse
import io.swagger.v3.oas.annotations.media.Schema

data class AccountPaymentTargetsResponse(
    @field:Schema(description = "납부 대상 요약")
    val summary: PaymentTargetSummaryResponse,
    @field:Schema(description = "납부 대상 페이지")
    val targets: PageResponse<AccountPaymentTargetResponse>,
) {
    data class PaymentTargetSummaryResponse(
        @field:Schema(description = "전체 활성 부원 수", example = "18")
        val totalCount: Int,
        @field:Schema(description = "선택된 납부 대상 수", example = "12")
        val targetedCount: Int,
        @field:Schema(description = "제외된 납부 대상 수", example = "6")
        val excludedCount: Int,
    )
}
