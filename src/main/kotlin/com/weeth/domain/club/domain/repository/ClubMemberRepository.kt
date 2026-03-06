package com.weeth.domain.club.domain.repository

import com.weeth.domain.club.application.exception.ClubMemberNotFoundException
import com.weeth.domain.club.domain.entity.ClubMember
import org.springframework.data.jpa.repository.JpaRepository

interface ClubMemberRepository :
    JpaRepository<ClubMember, Long>,
    ClubMemberReader {
    override fun getClubMemberById(clubMemberId: Long): ClubMember =
        findById(clubMemberId).orElseThrow { ClubMemberNotFoundException() }

    override fun findByIdOrNull(clubMemberId: Long): ClubMember? = findById(clubMemberId).orElse(null)

    override fun findByClubIdAndUserId(
        clubId: Long,
        userId: Long,
    ): ClubMember?

    override fun findAllByClubId(clubId: Long): List<ClubMember>

    override fun findAllByUserId(userId: Long): List<ClubMember>
}
