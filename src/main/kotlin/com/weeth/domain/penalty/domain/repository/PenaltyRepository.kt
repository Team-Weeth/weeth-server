package com.weeth.domain.penalty.domain.repository

import com.weeth.domain.penalty.domain.entity.Penalty
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface PenaltyRepository : JpaRepository<Penalty, Long> {
    @Query(
        "SELECT p FROM Penalty p JOIN FETCH p.clubMember cm JOIN FETCH cm.user JOIN FETCH p.cardinal WHERE cm.id = :clubMemberId AND p.cardinal.id = :cardinalId ORDER BY p.id DESC",
    )
    fun findByClubMemberIdAndCardinalIdOrderByIdDesc(
        clubMemberId: Long,
        cardinalId: Long,
    ): List<Penalty>

    @Query(
        "SELECT p FROM Penalty p JOIN FETCH p.clubMember cm JOIN FETCH cm.user JOIN FETCH p.cardinal WHERE cm.club.id = :clubId AND p.cardinal.id = :cardinalId ORDER BY p.id DESC",
    )
    fun findByClubIdAndCardinalIdOrderByIdDesc(
        clubId: Long,
        cardinalId: Long,
    ): List<Penalty>
}
