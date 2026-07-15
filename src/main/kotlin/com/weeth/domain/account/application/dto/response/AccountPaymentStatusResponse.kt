package com.weeth.domain.account.application.dto.response

import com.weeth.global.common.response.PageResponse
import io.swagger.v3.oas.annotations.media.Schema

data class AccountPaymentStatusResponse(
    @field:Schema(description = "납부현황 페이지 상단 요약")
    val summary: PaymentStatusSummaryResponse,
    @field:Schema(description = "부원별 납부현황 목록 (납부 대상만, 미납 순)")
    val members: PageResponse<AccountPaymentTargetResponse>,
) {
    data class PaymentStatusSummaryResponse(
        @field:Schema(description = "총 수납액 (납부 완료된 회비 합계, 원)", example = "1300000")
        val collectedAmount: Int,
        @field:Schema(description = "목표액 (납부 대상 회비 합계, 원)", example = "1390000")
        val targetAmount: Int,
        @field:Schema(description = "납부율 (0.0~1.0). 목표액이 0이면 null", example = "0.8", nullable = true)
        val paymentRate: Double?,
        @field:Schema(description = "납부 대상 수 (전체 탭)", example = "24")
        val targetCount: Int,
        @field:Schema(description = "납부 완료 수 (완료 탭)", example = "21")
        val paidCount: Int,
        @field:Schema(description = "미납 수 (미납 탭)", example = "3")
        val unpaidCount: Int,
        @field:Schema(description = "환불 수 (환불 탭)", example = "1")
        val refundedCount: Int,
        @field:Schema(description = "제외 수 (제외 탭, 활성 명부 − 활성 납부 대상)", example = "2")
        val excludedCount: Int,
        @field:Schema(description = "부원에게 계좌 공개 여부", example = "true")
        val bankAccountPublic: Boolean,
        @field:Schema(description = "입금 계좌 정보. 등록되지 않았으면 null", nullable = true)
        val bankAccount: BankAccountResponse?,
    )
}
