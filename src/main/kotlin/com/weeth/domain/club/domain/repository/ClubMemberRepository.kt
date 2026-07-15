package com.weeth.domain.club.domain.repository

import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.club.domain.enums.MemberRole
import com.weeth.domain.club.domain.enums.MemberStatus
import jakarta.persistence.LockModeType
import jakarta.persistence.QueryHint
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(QueryHint(name = "jakarta.persistence.lock.timeout", value = "2000"))
    @Query(
        """
        SELECT cm
        FROM ClubMember cm
        JOIN FETCH cm.user
        WHERE cm.user.id = :userId
        AND cm.memberStatus = com.weeth.domain.club.domain.enums.MemberStatus.ACTIVE
        ORDER BY cm.id ASC
        """,
    )
    fun findAllActiveByUserIdWithLock(
        @Param("userId") userId: Long,
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
        JOIN FETCH cm.club
        WHERE cm.user.id = :userId
        """,
    )
    override fun findAllByUserIdWithClub(
        @Param("userId") userId: Long,
    ): List<ClubMember>

    @Query(
        """
        SELECT cm
        FROM ClubMember cm
        JOIN FETCH cm.club
        WHERE cm.user.id = :userId
        AND cm.memberStatus = :memberStatus
        """,
    )
    override fun findAllByUserIdAndMemberStatusWithClub(
        @Param("userId") userId: Long,
        @Param("memberStatus") memberStatus: MemberStatus,
    ): List<ClubMember>

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
        value = """
        SELECT cm
        FROM ClubMember cm
        JOIN FETCH cm.user user
        WHERE cm.club.id = :clubId
        AND cm.memberStatus = com.weeth.domain.club.domain.enums.MemberStatus.ACTIVE
        AND (:keyword IS NULL OR user.name LIKE CONCAT('%', :keyword, '%'))
        ORDER BY cm.id ASC
        """,
        countQuery = """
        SELECT COUNT(cm)
        FROM ClubMember cm
        JOIN cm.user user
        WHERE cm.club.id = :clubId
        AND cm.memberStatus = com.weeth.domain.club.domain.enums.MemberStatus.ACTIVE
        AND (:keyword IS NULL OR user.name LIKE CONCAT('%', :keyword, '%'))
        """,
    )
    override fun findActiveByClubIdAndKeyword(
        @Param("clubId") clubId: Long,
        @Param("keyword") keyword: String?,
        pageable: Pageable,
    ): Page<ClubMember>

    @Query(
        value = """
        SELECT cm
        FROM ClubMember cm
        JOIN FETCH cm.user user
        WHERE cm.club.id = :clubId
        AND cm.memberStatus = com.weeth.domain.club.domain.enums.MemberStatus.ACTIVE
        AND (:keyword IS NULL OR user.name LIKE CONCAT('%', :keyword, '%'))
        AND NOT EXISTS (
            SELECT target.id
            FROM AccountPaymentTarget target
            WHERE target.account.id = :accountId
            AND target.clubMember = cm
            AND target.targetStatus = com.weeth.domain.account.domain.enums.AccountTargetStatus.TARGETED
        )
        ORDER BY cm.id ASC
        """,
        countQuery = """
        SELECT COUNT(cm)
        FROM ClubMember cm
        JOIN cm.user user
        WHERE cm.club.id = :clubId
        AND cm.memberStatus = com.weeth.domain.club.domain.enums.MemberStatus.ACTIVE
        AND (:keyword IS NULL OR user.name LIKE CONCAT('%', :keyword, '%'))
        AND NOT EXISTS (
            SELECT target.id
            FROM AccountPaymentTarget target
            WHERE target.account.id = :accountId
            AND target.clubMember = cm
            AND target.targetStatus = com.weeth.domain.account.domain.enums.AccountTargetStatus.TARGETED
        )
        """,
    )
    override fun findExcludedPaymentTargetCandidates(
        @Param("clubId") clubId: Long,
        @Param("accountId") accountId: Long,
        @Param("keyword") keyword: String?,
        pageable: Pageable,
    ): Page<ClubMember>

    @Query(
        """
        SELECT COUNT(cm)
        FROM ClubMember cm
        WHERE cm.club.id = :clubId
        AND cm.memberStatus = com.weeth.domain.club.domain.enums.MemberStatus.ACTIVE
        AND EXISTS (
            SELECT 1
            FROM ClubMemberCardinal cmc
            WHERE cmc.clubMember = cm
            AND cmc.cardinal.cardinalNumber = :cardinalNumber
        )
        """,
    )
    override fun countActiveByClubIdAndCardinalNumber(
        @Param("clubId") clubId: Long,
        @Param("cardinalNumber") cardinalNumber: Int,
    ): Long

    @Query(
        value = """
        SELECT cm
        FROM ClubMember cm
        JOIN FETCH cm.user user
        WHERE cm.club.id = :clubId
        AND cm.memberStatus = com.weeth.domain.club.domain.enums.MemberStatus.ACTIVE
        AND EXISTS (
            SELECT 1
            FROM ClubMemberCardinal cmc
            WHERE cmc.clubMember = cm
            AND cmc.cardinal.cardinalNumber = :cardinalNumber
        )
        AND (:keyword IS NULL OR user.name LIKE CONCAT('%', :keyword, '%'))
        ORDER BY cm.id ASC
        """,
        countQuery = """
        SELECT COUNT(cm)
        FROM ClubMember cm
        JOIN cm.user user
        WHERE cm.club.id = :clubId
        AND cm.memberStatus = com.weeth.domain.club.domain.enums.MemberStatus.ACTIVE
        AND EXISTS (
            SELECT 1
            FROM ClubMemberCardinal cmc
            WHERE cmc.clubMember = cm
            AND cmc.cardinal.cardinalNumber = :cardinalNumber
        )
        AND (:keyword IS NULL OR user.name LIKE CONCAT('%', :keyword, '%'))
        """,
    )
    override fun findActiveByClubIdAndCardinalNumberAndKeyword(
        @Param("clubId") clubId: Long,
        @Param("cardinalNumber") cardinalNumber: Int,
        @Param("keyword") keyword: String?,
        pageable: Pageable,
    ): Page<ClubMember>

    @Query(
        value = """
        SELECT cm
        FROM ClubMember cm
        JOIN FETCH cm.user user
        WHERE cm.club.id = :clubId
        AND cm.memberStatus = com.weeth.domain.club.domain.enums.MemberStatus.ACTIVE
        AND EXISTS (
            SELECT 1
            FROM ClubMemberCardinal cmc
            WHERE cmc.clubMember = cm
            AND cmc.cardinal.cardinalNumber = :cardinalNumber
        )
        AND (:keyword IS NULL OR user.name LIKE CONCAT('%', :keyword, '%'))
        AND NOT EXISTS (
            SELECT target.id
            FROM AccountPaymentTarget target
            WHERE target.account.id = :accountId
            AND target.clubMember = cm
            AND target.targetStatus = com.weeth.domain.account.domain.enums.AccountTargetStatus.TARGETED
        )
        ORDER BY cm.id ASC
        """,
        countQuery = """
        SELECT COUNT(cm)
        FROM ClubMember cm
        JOIN cm.user user
        WHERE cm.club.id = :clubId
        AND cm.memberStatus = com.weeth.domain.club.domain.enums.MemberStatus.ACTIVE
        AND EXISTS (
            SELECT 1
            FROM ClubMemberCardinal cmc
            WHERE cmc.clubMember = cm
            AND cmc.cardinal.cardinalNumber = :cardinalNumber
        )
        AND (:keyword IS NULL OR user.name LIKE CONCAT('%', :keyword, '%'))
        AND NOT EXISTS (
            SELECT target.id
            FROM AccountPaymentTarget target
            WHERE target.account.id = :accountId
            AND target.clubMember = cm
            AND target.targetStatus = com.weeth.domain.account.domain.enums.AccountTargetStatus.TARGETED
        )
        """,
    )
    override fun findExcludedPaymentTargetCandidatesByCardinal(
        @Param("clubId") clubId: Long,
        @Param("cardinalNumber") cardinalNumber: Int,
        @Param("accountId") accountId: Long,
        @Param("keyword") keyword: String?,
        pageable: Pageable,
    ): Page<ClubMember>

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

    @Query(
        """
        SELECT cm
        FROM ClubMember cm
        JOIN FETCH cm.user
        WHERE cm.club.id = :clubId
        AND cm.user.id IN :userIds
        """,
    )
    override fun findAllByClubIdAndUserIds(
        @Param("clubId") clubId: Long,
        @Param("userIds") userIds: List<Long>,
    ): List<ClubMember>
}
