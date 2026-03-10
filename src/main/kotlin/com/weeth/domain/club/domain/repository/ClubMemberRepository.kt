package com.weeth.domain.club.domain.repository

import com.weeth.domain.club.application.exception.ClubMemberNotFoundException
import com.weeth.domain.club.domain.entity.ClubMember
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ClubMemberRepository :
    JpaRepository<ClubMember, Long>,
    ClubMemberReader {
    override fun getClubMemberById(clubMemberId: Long): ClubMember =
        findById(clubMemberId).orElseThrow { ClubMemberNotFoundException() }

    override fun findByIdOrNull(clubMemberId: Long): ClubMember? = findById(clubMemberId).orElse(null)

    override fun findByIdAndClubId(
        clubMemberId: Long,
        clubId: Long,
    ): ClubMember?

    override fun findByClubIdAndUserId(
        clubId: Long,
        userId: Long,
    ): ClubMember?

    @Query(
        """
        SELECT cm
        FROM ClubMember cm
        JOIN FETCH cm.user
        WHERE cm.club.id = :clubId
        """,
    )
    override fun findAllByClubId(
        @Param("clubId") clubId: Long,
    ): List<ClubMember>

    override fun findAllByUserId(userId: Long): List<ClubMember>

    @Query(
        """
        SELECT COUNT(cm)
        FROM ClubMember cm
        WHERE cm.club.id = :clubId
        AND cm.memberStatus = com.weeth.domain.club.domain.enums.MemberStatus.ACTIVE
        """,
    )
    override fun countActiveByClubId(
        @Param("clubId") clubId: Long,
    ): Long
}
