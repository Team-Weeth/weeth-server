package com.weeth.domain.club.domain.repository

import com.weeth.domain.club.domain.entity.ClubMember

interface ClubMemberReader {
    fun findByIdWithLock(clubMemberId: Long): ClubMember?

    fun findByIdOrNull(clubMemberId: Long): ClubMember?

    fun findByClubIdAndUserId(
        clubId: Long,
        userId: Long,
    ): ClubMember?

    fun findAllByClubId(clubId: Long): List<ClubMember>

    fun findAllByUserId(userId: Long): List<ClubMember>
}
