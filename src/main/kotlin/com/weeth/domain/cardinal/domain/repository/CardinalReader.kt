package com.weeth.domain.cardinal.domain.repository

import com.weeth.domain.cardinal.domain.entity.Cardinal

interface CardinalReader {
    fun getByCardinalNumber(cardinalNumber: Int): Cardinal

    fun getByYearAndSemester(
        year: Int,
        semester: Int,
    ): Cardinal

    fun findByIdOrNull(cardinalId: Long): Cardinal?

    fun findAllByCardinalNumberDesc(): List<Cardinal>
}
