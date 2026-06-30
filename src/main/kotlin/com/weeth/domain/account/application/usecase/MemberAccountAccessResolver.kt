package com.weeth.domain.account.application.usecase

import com.weeth.domain.account.application.exception.AccountNotFoundException
import com.weeth.domain.account.domain.entity.Account
import com.weeth.domain.account.domain.enums.AccountStatus
import com.weeth.domain.account.domain.repository.AccountRepository
import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import org.springframework.stereotype.Component

/**
 * 유저 사이드 회비 화면 접근 게이트.
 * 부원용 QueryService 전반에서 동일하게 적용되는 두 단계 검증을 한 곳으로 모은다.
 *
 * 1. 활성 부원만 통과 — 탈퇴/비활성/비소속이면 [ClubMemberPolicy] 가 예외를 던진다.
 * 2. 공개(`memberVisible=true`)·`ACTIVE` 장부만 노출 — 없으면 [AccountNotFoundException].
 *    이 한 줄로 (a)없는 기수 (b)초안 상태 (c)미공개 장부를 모두 404로 은닉한다.
 */
@Component
class MemberAccountAccessResolver(
    private val accountRepository: AccountRepository,
    private val clubMemberPolicy: ClubMemberPolicy,
) {
    fun resolve(
        clubId: Long,
        cardinal: Int,
        userId: Long,
    ): MemberAccountAccess {
        val member = clubMemberPolicy.getActiveMember(clubId, userId)
        val account =
            accountRepository.findByClubIdAndCardinalAndStatusAndMemberVisibleTrue(
                clubId,
                cardinal,
                AccountStatus.ACTIVE,
            ) ?: throw AccountNotFoundException()
        return MemberAccountAccess(account, member)
    }
}

data class MemberAccountAccess(
    val account: Account,
    val member: ClubMember,
)
