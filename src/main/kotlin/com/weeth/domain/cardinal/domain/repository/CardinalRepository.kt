package com.weeth.domain.cardinal.domain.repository

import com.weeth.domain.cardinal.application.exception.CardinalNotFoundException
import com.weeth.domain.cardinal.domain.entity.Cardinal
import com.weeth.domain.cardinal.domain.enums.CardinalStatus
import jakarta.persistence.LockModeType
import jakarta.persistence.QueryHint
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints

interface CardinalRepository :
    JpaRepository<Cardinal, Long>,
    CardinalReader {
    fun findByCardinalNumber(cardinal: Int): Cardinal?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(QueryHint(name = "jakarta.persistence.lock.timeout", value = "2000"))
    @Query("SELECT c FROM Cardinal c WHERE c.club.id = :clubId AND c.status = 'IN_PROGRESS'")
    fun findAllInProgressByClubIdWithLock(clubId: Long): List<Cardinal>

    fun findAllByOrderByCardinalNumberDesc(): List<Cardinal>

    fun findByIdAndClubId(
        id: Long,
        clubId: Long,
    ): Cardinal?

    override fun findByClubIdAndCardinalNumber(
        clubId: Long,
        cardinalNumber: Int,
    ): Cardinal?

    override fun findAllByClubIdAndStatus(
        clubId: Long,
        status: CardinalStatus,
    ): List<Cardinal>

    override fun findAllByClubIdOrderByCardinalNumberAsc(clubId: Long): List<Cardinal>

    fun findFirstByClubIdAndStatusOrderByCardinalNumberDesc(
        clubId: Long,
        status: CardinalStatus,
    ): Cardinal?

    override fun findInProgressByClubId(clubId: Long): Cardinal? =
        findFirstByClubIdAndStatusOrderByCardinalNumberDesc(clubId, CardinalStatus.IN_PROGRESS)

    override fun getByCardinalNumber(cardinalNumber: Int): Cardinal =
        findByCardinalNumber(cardinalNumber) ?: throw CardinalNotFoundException()

    override fun findByIdOrNull(cardinalId: Long): Cardinal? = findById(cardinalId).orElse(null)

    override fun findAllByCardinalNumberDesc(): List<Cardinal> = findAllByOrderByCardinalNumberDesc()
}
