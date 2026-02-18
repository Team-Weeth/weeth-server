package com.weeth.domain.penalty.domain.repository

import com.weeth.domain.penalty.domain.entity.Penalty
import com.weeth.domain.penalty.domain.entity.enums.PenaltyType
import com.weeth.domain.user.domain.entity.Cardinal
import com.weeth.domain.user.domain.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

interface PenaltyRepository : JpaRepository<Penalty, Long> {
    fun findByUserIdAndCardinalIdOrderByIdDesc(
        userId: Long,
        cardinalId: Long,
    ): List<Penalty>

    fun findFirstByUserAndCardinalAndPenaltyTypeAndCreatedAtAfterOrderByCreatedAtAsc(
        user: User,
        cardinal: Cardinal,
        penaltyType: PenaltyType,
        createdAt: LocalDateTime,
    ): Penalty?

    fun findByCardinalIdOrderByIdDesc(cardinalId: Long): List<Penalty>
}
