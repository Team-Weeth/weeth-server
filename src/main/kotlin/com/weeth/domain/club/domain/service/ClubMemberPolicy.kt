package com.weeth.domain.club.domain.service

import com.weeth.domain.club.application.exception.ClubMemberNotFoundException
import com.weeth.domain.club.application.exception.ClubMemberNotInClubException
import com.weeth.domain.club.application.exception.MemberNotActiveException
import com.weeth.domain.club.application.exception.NotClubAdminException
import com.weeth.domain.club.domain.repository.ClubMemberReader
import org.springframework.stereotype.Service

/**
 * 동아리 멤버 관련 비즈니스 규칙 및 권한 검증
 */
@Service
class ClubMemberPolicy(
    private val clubMemberReader: ClubMemberReader,
) {
    /**
     * 동아리의 활성 멤버를 조회
     */
    fun getActiveMember(
        clubId: Long,
        userId: Long,
    ) = clubMemberReader
        .findByClubIdAndUserId(clubId, userId)
        ?.takeIf { it.isActive() }
        ?: throw if (clubMemberReader.findByClubIdAndUserId(clubId, userId) != null) {
            MemberNotActiveException()
        } else {
            ClubMemberNotFoundException()
        }

    /**
     * 사용자가 동아리 관리자인지 검증
     * 활성 상태이고 + 관리자 권한
     */
    fun requireAdmin(
        clubId: Long,
        userId: Long,
    ) = getActiveMember(clubId, userId).also {
        if (!it.isAdmin()) {
            throw NotClubAdminException()
        }
    }

    fun getMemberInClub(
        clubId: Long,
        clubMemberId: Long,
    ) = clubMemberReader.findByIdAndClubId(clubMemberId, clubId)
        ?: throw if (clubMemberReader.findByIdOrNull(clubMemberId) != null) {
            ClubMemberNotInClubException()
        } else {
            ClubMemberNotFoundException()
        }
}
