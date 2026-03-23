package com.weeth.domain.club.domain.repository

import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.club.domain.enums.MemberRole
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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(QueryHint(name = "jakarta.persistence.lock.timeout", value = "2000"))
    @Query("SELECT cm FROM ClubMember cm JOIN FETCH cm.user JOIN FETCH cm.club WHERE cm.id IN :ids ORDER BY cm.id ASC")
    override fun findAllByIdsWithLock(
        @Param("ids") ids: List<Long>,
    ): List<ClubMember>

    override fun findAllByClubIdAndMemberStatus(
        clubId: Long,
        memberStatus: MemberStatus,
    ): List<ClubMember>

    override fun findByIdOrNull(clubMemberId: Long): ClubMember? = findById(clubMemberId).orElse(null)

    override fun findByClubIdAndUserId(
        clubId: Long,
        userId: Long,
    ): ClubMember?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(QueryHint(name = "jakarta.persistence.lock.timeout", value = "2000"))
    @Query("SELECT cm FROM ClubMember cm WHERE cm.club.id = :clubId AND cm.user.id = :userId")
    override fun findByClubIdAndUserIdWithLock(
        @Param("clubId") clubId: Long,
        @Param("userId") userId: Long,
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
        SELECT cm
        FROM ClubMember cm
        WHERE cm.user.id = :userId
        AND cm.memberStatus = com.weeth.domain.club.domain.enums.MemberStatus.ACTIVE
        """,
    )
    override fun findActiveByUserId(
        @Param("userId") userId: Long,
    ): List<ClubMember>

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

    @Query(
        """
        SELECT COUNT(cm)
        FROM ClubMember cm
        WHERE cm.user.id = :userId
        AND cm.memberStatus = :memberStatus
        AND cm.memberRole = :memberRole
        """,
    )
    override fun countByUserIdAndMemberStatusAndMemberRole(
        @Param("userId") userId: Long,
        @Param("memberStatus") memberStatus: MemberStatus,
        @Param("memberRole") memberRole: MemberRole,
    ): Long
}
