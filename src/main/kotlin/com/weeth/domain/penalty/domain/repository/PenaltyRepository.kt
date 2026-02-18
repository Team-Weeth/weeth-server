package com.weeth.domain.penalty.domain.repository

import com.weeth.domain.penalty.domain.entity.Penalty
import com.weeth.domain.penalty.domain.enums.PenaltyType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime

interface PenaltyRepository : JpaRepository<Penalty, Long> {
    @Query("SELECT p FROM Penalty p JOIN FETCH p.cardinal WHERE p.user.id = :userId AND p.cardinal.id = :cardinalId ORDER BY p.id DESC")
    fun findByUserIdAndCardinalIdOrderByIdDesc(
        userId: Long,
        cardinalId: Long,
    ): List<Penalty>

    @Query(
        """
        SELECT p FROM Penalty p
        WHERE p.user.id = :userId AND p.cardinal.id = :cardinalId
        AND p.penaltyType = :penaltyType AND p.createdAt > :createdAt
        ORDER BY p.createdAt ASC
        LIMIT 1
    """,
    )
    fun findFirstAutoPenaltyAfter(
        userId: Long,
        cardinalId: Long,
        penaltyType: PenaltyType,
        createdAt: LocalDateTime,
    ): Penalty?

    @Query("SELECT p FROM Penalty p JOIN FETCH p.user JOIN FETCH p.cardinal WHERE p.cardinal.id = :cardinalId ORDER BY p.id DESC")
    fun findByCardinalIdOrderByIdDesc(cardinalId: Long): List<Penalty>
}
