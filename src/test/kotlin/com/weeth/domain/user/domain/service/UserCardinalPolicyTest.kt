package com.weeth.domain.user.domain.service

import com.weeth.domain.user.application.exception.CardinalNotFoundException
import com.weeth.domain.user.domain.repository.UserCardinalReader
import com.weeth.domain.user.fixture.CardinalTestFixture
import com.weeth.domain.user.fixture.UserCardinalTestFixture
import com.weeth.domain.user.fixture.UserTestFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class UserCardinalPolicyTest :
    DescribeSpec({
        val userCardinalReader = mockk<UserCardinalReader>()
        val policy = UserCardinalPolicy(userCardinalReader)

        describe("getCurrentCardinal") {
            it("가장 큰 기수 번호를 반환한다") {
                val user = UserTestFixture.createActiveUser1(1L)
                val cardinal5 = CardinalTestFixture.createCardinal(id = 2L, cardinalNumber = 5, year = 2025, semester = 1)

                every { userCardinalReader.findTopByUserOrderByCardinalNumberDesc(user) } returns
                    UserCardinalTestFixture.linkUserCardinal(user, cardinal5)

                policy.getCurrentCardinal(user).cardinalNumber shouldBe 5
            }

            it("기수 이력이 없으면 예외를 던진다") {
                val user = UserTestFixture.createActiveUser1(1L)
                every { userCardinalReader.findTopByUserOrderByCardinalNumberDesc(user) } returns null

                shouldThrow<CardinalNotFoundException> {
                    policy.getCurrentCardinal(user)
                }
            }
        }

        describe("notContains") {
            it("이미 포함된 기수면 false를 반환한다") {
                val user = UserTestFixture.createActiveUser1(1L)
                val cardinal = CardinalTestFixture.createCardinal(id = 2L, cardinalNumber = 5, year = 2025, semester = 1)
                every { userCardinalReader.findAllByUser(user) } returns listOf(UserCardinalTestFixture.linkUserCardinal(user, cardinal))

                policy.notContains(user, cardinal).shouldBeFalse()
            }
        }

        describe("isCurrent") {
            it("신규 기수가 현재 기수보다 크면 true를 반환한다") {
                val user = UserTestFixture.createActiveUser1(1L)
                val current = CardinalTestFixture.createCardinal(id = 1L, cardinalNumber = 4, year = 2024, semester = 2)
                val next = CardinalTestFixture.createCardinal(id = 2L, cardinalNumber = 5, year = 2025, semester = 1)
                every { userCardinalReader.findTopByUserOrderByCardinalNumberDesc(user) } returns
                    UserCardinalTestFixture.linkUserCardinal(user, current)

                policy.isCurrent(user, next).shouldBeTrue()
            }
        }
    })
