package com.weeth.domain.club.domain.repository

import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.club.domain.enums.MemberRole
import com.weeth.domain.club.domain.enums.MemberStatus

interface ClubMemberReader {
    fun findByIdWithLock(clubMemberId: Long): ClubMember?

    /**
     * 비관적 쓰기 락(PESSIMISTIC_WRITE)으로 여러 ClubMember를 조회한다.
     * 교착 방지를 위해 id 오름차순으로 락을 획득하며, 호출부에서도 [ids]를 정렬하여 전달해야 한다.
     */
    fun findAllByIdsWithLock(ids: List<Long>): List<ClubMember>

    fun findByIdOrNull(clubMemberId: Long): ClubMember?

    fun findByClubIdAndUserId(
        clubId: Long,
        userId: Long,
    ): ClubMember?

    fun findByClubIdAndUserIdWithLock(
        clubId: Long,
        userId: Long,
    ): ClubMember?

    fun findAllByClubId(clubId: Long): List<ClubMember>

    fun findAllByUserId(userId: Long): List<ClubMember>

    fun findActiveByUserId(userId: Long): List<ClubMember>

    fun countActiveByClubId(clubId: Long): Long

    fun findAllByClubIdAndMemberStatus(
        clubId: Long,
        memberStatus: MemberStatus,
    ): List<ClubMember>

    fun countByUserIdAndMemberStatusAndMemberRole(
        userId: Long,
        memberStatus: MemberStatus,
        memberRole: MemberRole,
    ): Long

    fun findAllByClubIdAndUserIds(
        clubId: Long,
        userIds: List<Long>,
    ): List<ClubMember>
}
