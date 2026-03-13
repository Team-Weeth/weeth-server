package com.weeth.domain.club.domain.repository

import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.club.domain.enums.MemberStatus
import jakarta.persistence.LockModeType
import jakarta.persistence.QueryHint
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import org.springframework.data.repository.query.Param

interface ClubMemberRepository :
    JpaRepository<ClubMember, Long>,
    ClubMemberReader {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(QueryHint(name = "jakarta.persistence.lock.timeout", value = "2000"))
    @Query("SELECT cm FROM ClubMember cm JOIN FETCH cm.user WHERE cm.id = :clubMemberId")
    override fun findByIdWithLock(
        @Param("clubMemberId") clubMemberId: Long,
    ): ClubMember?

    override fun findAllByClubIdAndMemberStatus(
        clubId: Long,
        memberStatus: MemberStatus,
    ): List<ClubMember>

    override fun findByIdOrNull(clubMemberId: Long): ClubMember? = findById(clubMemberId).orElse(null)

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
}
