package com.weeth.domain.account.application.dto.response

import com.weeth.domain.account.domain.enums.AccountPaymentStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class MyAccountResponse(
    @field:Schema(description = "회비 장부 ID", example = "12")
    val accountId: Long,
    @field:Schema(description = "회비 기수", example = "7")
    val cardinal: Int,
    @field:Schema(description = "회비 장부 이름", example = "7기 회비", nullable = true)
    val accountName: String?,
    @field:Schema(description = "1인 회비 금액", example = "60000")
    val duesAmount: Int,
    @field:Schema(description = "나의 납부 상태")
    val myPayment: MyPaymentResponse,
    @field:Schema(description = "부원에게 계좌 공개 여부", example = "true")
    val bankAccountVisible: Boolean,
    @field:Schema(description = "입금 계좌 정보. 비공개 또는 미등록이면 null", nullable = true)
    val bankAccount: BankAccountResponse?,
    @field:Schema(description = "잔액/목표액")
    val balance: BalanceResponse,
) {
    data class MyPaymentResponse(
        @field:Schema(description = "내가 이 회비의 납부 대상인지 여부", example = "true")
        val targeted: Boolean,
        @field:Schema(description = "나의 납부 상태. 납부 대상이 아니면 null", example = "UNPAID", nullable = true)
        val status: AccountPaymentStatus?,
        @field:Schema(description = "내 납부 대상 금액", example = "60000")
        val dueAmount: Int,
        @field:Schema(description = "실제 납부 완료 금액", example = "0")
        val paidAmount: Int,
        @field:Schema(description = "납부 확인 시각. 미납 또는 대상 아님이면 null", nullable = true)
        val paidAt: LocalDateTime?,
    )

    data class BalanceResponse(
        @field:Schema(description = "현재 남은 금액", example = "152129")
        val currentBalance: Int,
        @field:Schema(description = "목표 총액. 납부 대상 dueAmount 합계", example = "1425000")
        val goalAmount: Int,
    )
}
