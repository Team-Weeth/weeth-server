package com.weeth.domain.cardinal.domain.repository

import com.weeth.domain.cardinal.domain.entity.Cardinal
import com.weeth.domain.cardinal.domain.enums.CardinalStatus

interface CardinalReader {
    fun getByCardinalNumber(cardinalNumber: Int): Cardinal

    fun getByYearAndSemester(
        year: Int,
        semester: Int,
    ): Cardinal

    fun getByClubIdAndYearAndSemester(
        clubId: Long,
        year: Int,
        semester: Int,
    ): Cardinal

    fun findByIdOrNull(cardinalId: Long): Cardinal?

    fun findAllByCardinalNumberDesc(): List<Cardinal>

    fun findByClubIdAndCardinalNumber(
        clubId: Long,
        cardinalNumber: Int,
    ): Cardinal?

    fun findAllByClubIdAndStatus(
        clubId: Long,
        status: CardinalStatus,
    ): List<Cardinal>

    fun findAllByClubIdOrderByCardinalNumberAsc(clubId: Long): List<Cardinal>
}
