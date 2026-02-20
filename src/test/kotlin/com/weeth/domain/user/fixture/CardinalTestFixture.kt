package com.weeth.domain.user.fixture

import com.weeth.domain.user.domain.entity.Cardinal
import com.weeth.domain.user.domain.entity.enums.CardinalStatus

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
