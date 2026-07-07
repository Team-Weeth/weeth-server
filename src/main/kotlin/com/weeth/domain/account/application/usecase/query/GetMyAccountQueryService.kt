package com.weeth.domain.account.application.usecase.query

import com.weeth.domain.account.application.dto.response.AccountCardinalResponse
import com.weeth.domain.account.application.dto.response.MyAccountResponse
import com.weeth.domain.account.application.mapper.MyAccountMapper
import com.weeth.domain.account.application.usecase.MemberAccountAccessResolver
import com.weeth.domain.account.domain.enums.AccountStatus
import com.weeth.domain.account.domain.repository.AccountPaymentTargetRepository
import com.weeth.domain.account.domain.repository.AccountRepository
import com.weeth.domain.club.domain.repository.ClubMemberCardinalReader
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetMyAccountQueryService(
    private val accountRepository: AccountRepository,
    private val paymentTargetRepository: AccountPaymentTargetRepository,
    private val clubMemberPolicy: ClubMemberPolicy,
    private val clubMemberCardinalReader: ClubMemberCardinalReader,
    private val memberAccountAccessResolver: MemberAccountAccessResolver,
    private val myAccountMapper: MyAccountMapper,
) {
    fun findCardinals(
        clubId: Long,
        userId: Long,
    ): List<AccountCardinalResponse> {
        val member = clubMemberPolicy.getActiveMember(clubId, userId)
        val participatedCardinals =
            clubMemberCardinalReader
                .findAllByClubMember(member)
                .map { it.cardinal.cardinalNumber }
                .toSet()
        val accounts =
            accountRepository
                .findAllByClubIdAndStatusAndMemberVisibleTrueOrderByCardinalDesc(
                    clubId,
                    AccountStatus.ACTIVE,
                ).filter { it.cardinal in participatedCardinals }
        val latestCardinal = accounts.firstOrNull()?.cardinal

        return accounts.map { myAccountMapper.toCardinalResponse(it, isLatest = it.cardinal == latestCardinal) }
    }

    fun findMyAccount(
        clubId: Long,
        cardinal: Int,
        userId: Long,
    ): MyAccountResponse {
        val (account, member) = memberAccountAccessResolver.resolve(clubId, cardinal, userId)
        val target = paymentTargetRepository.findByAccountIdAndClubMemberId(account.id, member.id)
        val goalAmount = paymentTargetRepository.sumDueAmountByAccountId(account.id).toInt()

        return myAccountMapper.toResponse(account, target, goalAmount)
    }
}
