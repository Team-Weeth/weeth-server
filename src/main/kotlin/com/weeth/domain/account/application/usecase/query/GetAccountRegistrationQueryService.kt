package com.weeth.domain.account.application.usecase.query

import com.weeth.domain.account.application.dto.response.AccountCarryOverSourceResponse
import com.weeth.domain.account.application.exception.AccountNotFoundException
import com.weeth.domain.account.domain.enums.AccountStatus
import com.weeth.domain.account.domain.repository.AccountRepository
import com.weeth.domain.club.domain.service.ClubPermissionPolicy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetAccountRegistrationQueryService(
    private val accountRepository: AccountRepository,
    private val clubPermissionPolicy: ClubPermissionPolicy,
) {
    /** 이월 설정 단계의 재원 정보. 직전 활성 기수 장부의 잔액을 반환하고, 없으면 hasPreviousAccount=false 로 알린다. */
    fun findCarryOverSource(
        clubId: Long,
        accountId: Long,
        userId: Long,
    ): AccountCarryOverSourceResponse {
        clubPermissionPolicy.requireAdmin(clubId, userId)
        val account = accountRepository.findById(accountId).orElseThrow { AccountNotFoundException() }
        if (account.club.id != 0L && account.club.id != clubId) throw AccountNotFoundException()

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
}
