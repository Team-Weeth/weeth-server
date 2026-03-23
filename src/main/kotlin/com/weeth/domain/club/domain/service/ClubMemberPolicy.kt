package com.weeth.domain.club.domain.service

import com.weeth.domain.club.application.exception.ClubCreateLimitExceededException
import com.weeth.domain.club.application.exception.ClubJoinLimitExceededException
import com.weeth.domain.club.application.exception.ClubMemberNotFoundException
import com.weeth.domain.club.application.exception.ClubMemberNotInClubException
import com.weeth.domain.club.application.exception.MemberNotActiveException
import com.weeth.domain.club.application.exception.NotClubAdminException
import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.club.domain.enums.MemberRole
import com.weeth.domain.club.domain.enums.MemberStatus
import com.weeth.domain.club.domain.repository.ClubMemberReader
import org.springframework.stereotype.Service

/**
 * 동아리 멤버 관련 비즈니스 규칙 및 권한 검증
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

    /**
     * 사용자가 동아리 관리자인지 검증
     * 활성 상태이고 + 관리자 권한
     */
    fun requireAdmin(
        clubId: Long,
        userId: Long,
    ) = getActiveMember(clubId, userId).also {
        // TODO: 동아리 생성자를 LEAD로 저장하고 있어 LEAD도 관리자 권한으로 취급할지 정책 정리가 필요하다.
        if (!it.isAdmin()) {
            throw NotClubAdminException()
        }
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

    /**
     * 일반 멤버(USER)로 가입 가능한 동아리 수 제한 검증
     */
    fun validateJoinLimit(userId: Long) {
        val activeUserCount =
            clubMemberReader.countByUserIdAndMemberStatusAndMemberRole(
                userId,
                MemberStatus.ACTIVE,
                MemberRole.USER,
            )
        if (activeUserCount >= MAX_USER_CLUBS) {
            throw ClubJoinLimitExceededException()
        }
    }

    /**
     * 동아리장(LEAD)으로 생성 가능한 동아리 수 제한 검증
     */
    fun validateCreateLimit(userId: Long) {
        val activeLeadCount =
            clubMemberReader.countByUserIdAndMemberStatusAndMemberRole(
                userId,
                MemberStatus.ACTIVE,
                MemberRole.LEAD,
            )
        if (activeLeadCount >= MAX_LEAD_CLUBS) {
            throw ClubCreateLimitExceededException()
        }
    }

    companion object {
        private const val MAX_LEAD_CLUBS = 1
        private const val MAX_USER_CLUBS = 1
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
