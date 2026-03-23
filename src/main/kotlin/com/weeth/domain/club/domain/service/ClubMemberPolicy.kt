package com.weeth.domain.club.domain.service

import com.weeth.domain.club.application.exception.ClubMemberNotFoundException
import com.weeth.domain.club.application.exception.ClubMemberNotInClubException
import com.weeth.domain.club.application.exception.MemberNotActiveException
import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.club.domain.repository.ClubMemberReader
import org.springframework.stereotype.Service

/**
 * 동아리 멤버 조회 및 상태 검증
 * TODO: 캐싱 도입
 */
@Service
class ClubMemberPolicy(
    private val clubMemberReader: ClubMemberReader,
) {
    /**
     * 동아리의 활성 멤버를 조회
     * 한 번 조회 후 분기하여 불필요한 중복 쿼리를 방지
     */
    fun getActiveMember(
        clubId: Long,
        userId: Long,
    ): ClubMember {
        val member =
            clubMemberReader.findByClubIdAndUserId(clubId, userId)
                ?: throw ClubMemberNotFoundException()
        if (!member.isActive()) throw MemberNotActiveException()
        return member
    }

    fun getActiveMemberWithLock(
        clubId: Long,
        userId: Long,
    ): ClubMember {
        val member =
            clubMemberReader.findByClubIdAndUserIdWithLock(clubId, userId)
                ?: throw ClubMemberNotFoundException()
        if (!member.isActive()) throw MemberNotActiveException()
        return member
    }

    fun getMemberInClub(
        clubId: Long,
        clubMemberId: Long,
    ): ClubMember {
        val member =
            clubMemberReader.findByIdOrNull(clubMemberId)
                ?: throw ClubMemberNotFoundException()
        if (member.club.id != clubId) throw ClubMemberNotInClubException()
        return member
    }

    fun getActiveMemberInClubWithLock(
        clubId: Long,
        clubMemberId: Long,
    ): ClubMember {
        val member =
            clubMemberReader.findByIdWithLock(clubMemberId)
                ?: throw ClubMemberNotFoundException()
        if (member.club.id != clubId) throw ClubMemberNotInClubException()
        if (!member.isActive()) throw MemberNotActiveException()
        return member
    }
}
