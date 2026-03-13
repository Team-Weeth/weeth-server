package com.weeth.domain.penalty.domain.repository

import com.weeth.domain.penalty.domain.entity.Penalty
import jakarta.persistence.LockModeType
import jakarta.persistence.QueryHint
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import org.springframework.data.repository.query.Param

interface PenaltyRepository : JpaRepository<Penalty, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(QueryHint(name = "jakarta.persistence.lock.timeout", value = "2000"))
    @Query("SELECT p FROM Penalty p WHERE p.id = :penaltyId")
    fun findByIdWithLock(
        @Param("penaltyId") penaltyId: Long,
    ): Penalty?

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
