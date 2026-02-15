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
        Cardinal
            .builder()
            .id(id)
            .cardinalNumber(cardinalNumber)
            .year(year)
            .semester(semester)
            .status(CardinalStatus.DONE)
            .build()

    fun createCardinalInProgress(
        id: Long? = null,
        cardinalNumber: Int,
        year: Int,
        semester: Int,
    ): Cardinal =
        Cardinal
            .builder()
            .id(id)
            .cardinalNumber(cardinalNumber)
            .year(year)
            .semester(semester)
            .status(CardinalStatus.IN_PROGRESS)
            .build()
}
