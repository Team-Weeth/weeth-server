package com.weeth.domain.user.domain.repository

import com.weeth.config.TestContainersConfig
import com.weeth.domain.user.fixture.CardinalTestFixture
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.optional.shouldBePresent
import io.kotest.matchers.shouldBe
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import

@DataJpaTest
@Import(TestContainersConfig::class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CardinalRepositoryTest(
    private val cardinalRepository: CardinalRepository,
) : StringSpec({

        "기수번호로 조회된다" {
            val cardinal = CardinalTestFixture.createCardinal(cardinalNumber = 7, year = 2025, semester = 1)
            cardinalRepository.save(cardinal)

            val result = cardinalRepository.findByCardinalNumber(7)

            result.shouldBePresent {
                it.year shouldBe 2025
            }
        }
    })
