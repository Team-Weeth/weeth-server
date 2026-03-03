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
import java.util.Optional

interface CardinalRepository :
    JpaRepository<Cardinal, Long>,
    CardinalReader {
    fun findByCardinalNumber(cardinal: Int): Optional<Cardinal>

    fun findAllByCardinalNumberIn(cardinalNumbers: List<Int>): List<Cardinal>

    fun findByYearAndSemester(
        year: Int,
        semester: Int,
    ): Optional<Cardinal>

    fun findAllByStatus(cardinalStatus: CardinalStatus): List<Cardinal>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(QueryHint(name = "jakarta.persistence.lock.timeout", value = "2000"))
    @Query("SELECT c FROM Cardinal c WHERE c.status = 'IN_PROGRESS'")
    fun findAllInProgressWithLock(): List<Cardinal>

    fun findAllByOrderByCardinalNumberAsc(): List<Cardinal>

    fun findAllByOrderByCardinalNumberDesc(): List<Cardinal>

    override fun getByCardinalNumber(cardinalNumber: Int): Cardinal =
        findByCardinalNumber(cardinalNumber).orElseThrow { CardinalNotFoundException() }

    override fun getByYearAndSemester(
        year: Int,
        semester: Int,
    ): Cardinal = findByYearAndSemester(year, semester).orElseThrow { CardinalNotFoundException() }

    override fun findByIdOrNull(cardinalId: Long): Cardinal? = findById(cardinalId).orElse(null)

    override fun findAllByCardinalNumberDesc(): List<Cardinal> = findAllByOrderByCardinalNumberDesc()
}
