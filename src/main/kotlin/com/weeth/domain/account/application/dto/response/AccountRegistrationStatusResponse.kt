package com.weeth.domain.account.application.dto.response

import com.weeth.domain.account.domain.enums.AccountRegistrationStep
import io.swagger.v3.oas.annotations.media.Schema

data class AccountRegistrationStatusResponse(
    @field:Schema(description = "회비 장부 ID")
    val accountId: Long,
    @field:Schema(
        description =
            "다음에 작성할 단계. 이어서 작성 시 이 단계 화면에서 시작하고, " +
                "스텝퍼에서 이 단계 이전은 완료(✓) 표시해주세요. " +
                "(BASIC → PAYMENT_TARGETS → CARRY_OVER → BANK_ACCOUNT → REVIEW 순)",
        example = "CARRY_OVER",
    )
    val registrationStep: AccountRegistrationStep,
    @field:Schema(description = "기본 정보 (BASIC 단계 저장 후 non-null). 값으로 폼을 채우고, null이면 빈 폼으로 시작해주세요.", nullable = true)
    val basic: BasicInfoResponse?,
    @field:Schema(
        description = "이월 설정 (CARRY_OVER 단계 저장 후 non-null). 값으로 폼을 채우고, null이면 빈 폼으로 시작해주세요.",
        nullable = true,
    )
    val carryOver: CarryOverResponse?,
    @field:Schema(
        description =
            "납부 대상 설정 요약 (PAYMENT_TARGETS 단계 저장 후 non-null). " +
                "대상 목록과 체크 상태는 납부 대상 목록 조회 API로 별도 조회해주세요.",
        nullable = true,
    )
    val paymentTargets: PaymentTargetsResponse?,
    @field:Schema(
        description = "계좌 설정 (BANK_ACCOUNT 단계 저장 후 non-null). 값으로 폼을 채우고, null이면 빈 폼으로 시작해주세요.",
        nullable = true,
    )
    val bankAccount: BankAccountRegistrationResponse?,
    @field:Schema(description = "이전 기수 장부 잔액. 이월 설정 단계의 안내 문구에 사용해주세요. 이전 장부가 없으면 null", nullable = true)
    val previousAccountBalance: PreviousAccountBalanceResponse?,
) {
    data class BasicInfoResponse(
        @field:Schema(description = "회비 장부 이름", example = "5기 정기 회비")
        val name: String,
        @field:Schema(description = "1인 회비 금액 (원)", example = "50000")
        val duesAmount: Int,
        @field:Schema(description = "회비 설명", example = "동아리 운영비로 사용됩니다.")
        val description: String?,
    )

    data class CarryOverResponse(
        @field:Schema(description = "이월 활성화 여부", example = "true")
        val enabled: Boolean,
        @field:Schema(description = "이월 금액 (원). enabled=false 이면 0", example = "240000")
        val amount: Int,
        @field:Schema(description = "이월 메모", example = "3기 잔액", nullable = true)
        val memo: String?,
    )

    data class PaymentTargetsResponse(
        @field:Schema(description = "납부 대상 인원 수", example = "20")
        val targetCount: Int,
        @field:Schema(description = "납부 제외 인원 수", example = "2")
        val excludedCount: Int,
    )

    data class BankAccountRegistrationResponse(
        @field:Schema(description = "계좌 공개 여부", example = "true")
        val bankAccountVisible: Boolean,
        @field:Schema(description = "입금 계좌 정보. bankAccountVisible=false 이면 null", nullable = true)
        val bankAccount: BankAccountResponse?,
    )

    data class PreviousAccountBalanceResponse(
        @field:Schema(description = "이전 기수", example = "3")
        val cardinalNumber: Int,
        @field:Schema(description = "이전 기수 장부 잔액", example = "240000")
        val balance: Int,
    )
}
