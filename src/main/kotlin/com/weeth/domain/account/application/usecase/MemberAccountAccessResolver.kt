package com.weeth.domain.account.application.usecase

import com.weeth.domain.account.application.exception.AccountFeatureNotPublicException
import com.weeth.domain.account.application.exception.AccountNotFoundException
import com.weeth.domain.account.domain.entity.Account
import com.weeth.domain.account.domain.enums.AccountStatus
import com.weeth.domain.account.domain.repository.AccountRepository
import com.weeth.domain.account.domain.repository.AccountSettingRepository
import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.club.domain.repository.ClubMemberCardinalReader
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import org.springframework.stereotype.Component

/**
 * 유저 사이드 회비 화면 접근 게이트.
 * 부원용 QueryService 전반에서 동일하게 적용되는 세 단계 검증을 한 곳으로 모은다.
 *
 * 1. 활성 부원만 통과 — 탈퇴/비활성/비소속이면 [ClubMemberPolicy] 가 예외를 던진다.
 * 2. 회비 기능이 club 단위로 공개(`AccountSetting.memberVisible=true`)일 때만 통과 — 비공개면 [AccountFeatureNotPublicException].
 *    "기능 자체가 꺼짐"을 전용 코드로 알려 (a)(b) 의 개별 장부 은닉(404)과 구분한다.
 * 3. `ACTIVE` 장부만 노출 — 없으면 [AccountNotFoundException] 으로 (a)없는 기수 (b)초안 상태를 404로 은닉한다.
 * 4. 내가 참여한 기수의 장부만 노출 — 참여하지 않은 기수는 [AccountNotFoundException] 으로 은닉한다.
 *    목록 필터를 우회한 기수 번호 직접 접근을 여기서 차단한다.
 */
@Component
class MemberAccountAccessResolver(
    private val accountRepository: AccountRepository,
    private val accountSettingRepository: AccountSettingRepository,
    private val clubMemberPolicy: ClubMemberPolicy,
    private val clubMemberCardinalReader: ClubMemberCardinalReader,
) {
    fun resolve(
        clubId: Long,
        cardinal: Int,
        userId: Long,
    ): MemberAccountAccess {
        val member = clubMemberPolicy.getActiveMember(clubId, userId)
        if (!accountSettingRepository.isVisibleToMembers(clubId)) throw AccountFeatureNotPublicException()

        val account =
            accountRepository.findByClubIdAndCardinalAndStatus(
                clubId,
                cardinal,
                AccountStatus.ACTIVE,
            ) ?: throw AccountNotFoundException()

        val participated =
            clubMemberCardinalReader
                .findAllByClubMember(member)
                .any { it.cardinal.cardinalNumber == cardinal }
        if (!participated) throw AccountNotFoundException()

        return MemberAccountAccess(account, member)
    }
}

data class MemberAccountAccess(
    val account: Account,
    val member: ClubMember,
)
