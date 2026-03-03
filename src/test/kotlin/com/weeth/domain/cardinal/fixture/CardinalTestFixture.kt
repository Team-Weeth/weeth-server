package com.weeth.domain.cardinal.fixture

import com.weeth.domain.cardinal.domain.entity.Cardinal
import com.weeth.domain.cardinal.domain.enums.CardinalStatus

object CardinalTestFixture {
    fun createCardinal(
        id: Long? = null,
        cardinalNumber: Int,
        year: Int,
        semester: Int,
    ): Cardinal =
        Cardinal(
            id = id ?: 0L,
            cardinalNumber = cardinalNumber,
            year = year,
            semester = semester,
            status = CardinalStatus.DONE,
        )

    fun createCardinalInProgress(
        id: Long? = null,
        cardinalNumber: Int,
        year: Int,
        semester: Int,
    ): Cardinal =
        Cardinal(
            id = id ?: 0L,
            cardinalNumber = cardinalNumber,
            year = year,
            semester = semester,
            status = CardinalStatus.IN_PROGRESS,
        )
}
