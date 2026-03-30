package com.weeth.domain.cardinal.domain.entity

import com.weeth.domain.cardinal.domain.enums.CardinalStatus
import com.weeth.domain.club.fixture.ClubTestFixture
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class CardinalTest :
    StringSpec({
        val club = ClubTestFixture.createClub()

        "inProgress/done 상태 전환" {
            val cardinal = Cardinal(club = club, cardinalNumber = 10)

            cardinal.inProgress()
            cardinal.status shouldBe CardinalStatus.IN_PROGRESS

            cardinal.done()
            cardinal.status shouldBe CardinalStatus.DONE
        }
    })
