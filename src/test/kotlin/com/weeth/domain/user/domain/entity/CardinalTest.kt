package com.weeth.domain.user.domain.entity

import com.weeth.domain.user.domain.entity.enums.CardinalStatus
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class CardinalTest :
    StringSpec({
        "inProgress/done 상태 전환" {
            val cardinal = Cardinal(cardinalNumber = 10, year = 2026, semester = 1)

            cardinal.inProgress()
            cardinal.status shouldBe CardinalStatus.IN_PROGRESS

            cardinal.done()
            cardinal.status shouldBe CardinalStatus.DONE
        }

        "update는 year/semester를 변경한다" {
            val cardinal = Cardinal(cardinalNumber = 9, year = 2025, semester = 2)
            cardinal.update(2026, 1)

            cardinal.year shouldBe 2026
            cardinal.semester shouldBe 1
        }
    })
