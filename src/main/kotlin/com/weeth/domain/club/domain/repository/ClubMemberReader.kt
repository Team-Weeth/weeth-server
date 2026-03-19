package com.weeth.domain.club.domain.repository

import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.club.domain.enums.MemberStatus

interface ClubMemberReader {
    fun findByIdWithLock(clubMemberId: Long): ClubMember?

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
}
