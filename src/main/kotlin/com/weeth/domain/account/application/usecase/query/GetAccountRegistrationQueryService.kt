package com.weeth.domain.account.application.usecase.query

import com.weeth.domain.account.application.dto.response.AccountCarryOverSourceResponse
import com.weeth.domain.account.application.dto.response.AccountRegistrationStatusResponse
import com.weeth.domain.account.application.exception.AccountNotFoundException
import com.weeth.domain.account.application.mapper.AccountRegistrationMapper
import com.weeth.domain.account.application.usecase.validateOwnedBy
import com.weeth.domain.account.domain.enums.AccountRegistrationStep
import com.weeth.domain.account.domain.enums.AccountStatus
import com.weeth.domain.account.domain.enums.AccountTargetStatus
import com.weeth.domain.account.domain.repository.AccountPaymentTargetRepository
import com.weeth.domain.account.domain.repository.AccountRepository
import com.weeth.domain.club.domain.repository.ClubMemberReader
import com.weeth.domain.club.domain.service.ClubPermissionPolicy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetAccountRegistrationQueryService(
    private val accountRepository: AccountRepository,
    private val paymentTargetRepository: AccountPaymentTargetRepository,
    private val clubMemberReader: ClubMemberReader,
    private val clubPermissionPolicy: ClubPermissionPolicy,
    private val registrationMapper: AccountRegistrationMapper,
) {
    /** 이월 설정 단계의 재원 정보. 직전 활성 기수 장부의 잔액을 반환하고, 없으면 hasPreviousAccount=false 로 알린다. */
    fun findCarryOverSource(
        clubId: Long,
        accountId: Long,
        userId: Long,
    ): AccountCarryOverSourceResponse {
        clubPermissionPolicy.requireAdmin(clubId, userId)
        val account = accountRepository.findById(accountId).orElseThrow { AccountNotFoundException() }
        account.validateOwnedBy(clubId)

        val previousAccount =
            accountRepository.findTopByClubIdAndCardinalLessThanAndStatusOrderByCardinalDesc(
                clubId = clubId,
                cardinal = account.cardinal,
                status = AccountStatus.ACTIVE,
            )

        return AccountCarryOverSourceResponse(
            hasPreviousAccount = previousAccount != null,
            cardinalNumber = previousAccount?.cardinal,
            balance = previousAccount?.currentBalance,
        )
    }

    fun findStatus(
        clubId: Long,
        accountId: Long,
        userId: Long,
    ): AccountRegistrationStatusResponse {
        clubPermissionPolicy.requireAdmin(clubId, userId)
        val account = accountRepository.findById(accountId).orElseThrow { AccountNotFoundException() }
        account.validateOwnedBy(clubId)

        // 납부 대상 화면과 동일하게 활성 기수 명부 기준으로 집계한다.
        // (행 없음 = 제외, 탈퇴/퇴출 멤버의 행은 카운트에서 제외)
        val paymentTargetCount =
            if (account.registrationStep.isAtLeast(AccountRegistrationStep.CARRY_OVER)) {
                paymentTargetRepository
                    .countActiveClubMemberTargetsByAccountIdAndTargetStatus(
                        accountId = accountId,
                        targetStatus = AccountTargetStatus.TARGETED,
                    ).toInt()
            } else {
                null
            }

        val excludedTargetCount =
            paymentTargetCount?.let { targeted ->
                val rosterCount =
                    clubMemberReader
                        .countActiveByClubIdAndCardinalNumber(clubId, account.cardinal)
                        .toInt()
                (rosterCount - targeted).coerceAtLeast(0)
            }

        val previousAccount =
            accountRepository.findTopByClubIdAndCardinalLessThanAndStatusOrderByCardinalDesc(
                clubId = clubId,
                cardinal = account.cardinal,
                status = AccountStatus.ACTIVE,
            )

        return registrationMapper.toResponse(
            account = account,
            paymentTargetCount = paymentTargetCount,
            excludedTargetCount = excludedTargetCount,
            previousAccount = previousAccount,
        )
    }
}
