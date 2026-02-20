package com.weeth.domain.user.domain.repository

import com.weeth.domain.user.application.exception.CardinalNotFoundException
import com.weeth.domain.user.domain.entity.Cardinal
import com.weeth.domain.user.domain.entity.enums.CardinalStatus
import org.springframework.data.jpa.repository.JpaRepository
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

    fun findFirstByStatusOrderByCardinalNumberDesc(status: CardinalStatus): Cardinal?

    fun findAllByOrderByCardinalNumberAsc(): List<Cardinal>

    fun findAllByOrderByCardinalNumberDesc(): List<Cardinal>

    override fun getByCardinalNumber(cardinalNumber: Int): Cardinal =
        findByCardinalNumber(cardinalNumber).orElseThrow { CardinalNotFoundException() }

    override fun findByIdOrNull(cardinalId: Long): Cardinal? = findById(cardinalId).orElse(null)

    override fun findAllByCardinalNumberDesc(): List<Cardinal> = findAllByOrderByCardinalNumberDesc()
}
