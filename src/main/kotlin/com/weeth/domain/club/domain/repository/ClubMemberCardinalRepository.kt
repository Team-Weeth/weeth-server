package com.weeth.domain.club.domain.repository

import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.club.domain.entity.ClubMemberCardinal
import com.weeth.domain.club.domain.enums.MemberStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ClubMemberCardinalRepository :
    JpaRepository<ClubMemberCardinal, Long>,
    ClubMemberCardinalReader {
    override fun findAllByClubMember(clubMember: ClubMember): List<ClubMemberCardinal>

    fun existsByClubMember(clubMember: ClubMember): Boolean

    @Query(
        """
        SELECT cmc
        FROM ClubMemberCardinal cmc
        JOIN FETCH cmc.cardinal
        WHERE cmc.clubMember IN :clubMembers
        """,
    )
    override fun findAllByClubMembers(
        @Param("clubMembers") clubMembers: List<ClubMember>,
    ): List<ClubMemberCardinal>

    @Query(
        "SELECT cmc FROM ClubMemberCardinal cmc " +
            "JOIN cmc.cardinal c " +
            "WHERE cmc.clubMember = :clubMember " +
            "ORDER BY c.cardinalNumber DESC " +
            "LIMIT 1",
    )
    fun findTopByClubMemberOrderedByCardinalNumberDesc(clubMember: ClubMember): ClubMemberCardinal?

    override fun findLatestCardinalByClubMember(clubMember: ClubMember): ClubMemberCardinal? =
        findTopByClubMemberOrderedByCardinalNumberDesc(clubMember)

    @Query(
        """
        SELECT cmc
        FROM ClubMemberCardinal cmc
        JOIN FETCH cmc.clubMember cm
        JOIN FETCH cm.club
        JOIN FETCH cm.user
        JOIN FETCH cmc.cardinal
        WHERE cm.club.id = :clubId
        AND cmc.cardinal.cardinalNumber = :cardinalNumber
        AND cm.memberStatus = :status
        """,
    )
    override fun findAllByClubIdAndCardinalNumber(
        @Param("clubId") clubId: Long,
        @Param("cardinalNumber") cardinalNumber: Int,
        @Param("status") status: MemberStatus,
    ): List<ClubMemberCardinal>

    override fun existsByClubMemberAndCardinalId(
        clubMember: ClubMember,
        cardinalId: Long,
    ): Boolean
}
