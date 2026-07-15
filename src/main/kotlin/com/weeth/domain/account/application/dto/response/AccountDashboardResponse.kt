package com.weeth.domain.account.application.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class AccountDashboardResponse(
    @field:Schema(description = "회비 장부 ID. 다른 회비 어드민 API 호출에 사용", example = "1")
    val accountId: Long,
    @field:Schema(description = "잔액/총액 요약")
    val summary: SummaryResponse,
    @field:Schema(description = "납부 현황 요약 (예: 3 / 24)")
    val paymentSummary: PaymentSummaryResponse,
    @field:Schema(description = "부원에게 회비 공개 여부", example = "true")
    val memberVisible: Boolean,
    @field:Schema(description = "부원에게 계좌 공개 여부", example = "true")
    val bankAccountPublic: Boolean,
    @field:Schema(description = "입금 계좌 정보. 등록되지 않았으면 null", nullable = true)
    val bankAccount: BankAccountResponse?,
    @field:Schema(description = "마지막 수정 정보")
    val lastModified: LastModifiedResponse,
    @field:Schema(description = "이 기수 장부의 활동 구간")
    val period: PeriodResponse,
    @field:Schema(description = "period 의 월 수만큼의 월별 잔액 추이")
    val monthlyBalances: List<MonthlyBalanceResponse>,
) {
    data class SummaryResponse(
        @field:Schema(description = "총 회비 금액 (원)", example = "240000")
        val totalAmount: Int,
        @field:Schema(description = "현재 잔액 (원)", example = "190000")
        val currentBalance: Int,
    )

    data class LastModifiedResponse(
        @field:Schema(description = "마지막 수정 시각")
        val modifiedAt: LocalDateTime,
        @field:Schema(description = "마지막 수정자. 기록이 없으면 null", nullable = true)
        val modifiedBy: ModifierResponse?,
    )

    data class ModifierResponse(
        @field:Schema(description = "사용자 ID", example = "1")
        val userId: Long,
        @field:Schema(description = "사용자 이름", example = "홍길동")
        val name: String,
        @field:Schema(description = "부원 프로필 이미지 URL. 미설정 시 null", nullable = true)
        val profileImageUrl: String?,
    )

    data class PeriodResponse(
        @field:Schema(description = "활동 시작 월 (yyyy-MM)", example = "2026-07")
        val startYearMonth: String,
        @field:Schema(description = "활동 종료 월 (yyyy-MM)", example = "2026-12")
        val endYearMonth: String,
    )
}
