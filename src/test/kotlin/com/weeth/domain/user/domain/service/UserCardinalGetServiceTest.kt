package com.weeth.domain.user.domain.service

import com.weeth.domain.user.application.exception.CardinalNotFoundException
import com.weeth.domain.user.domain.entity.UserCardinal
import com.weeth.domain.user.domain.repository.UserCardinalRepository
import com.weeth.domain.user.fixture.CardinalTestFixture
import com.weeth.domain.user.fixture.UserTestFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.mockk.every
import io.mockk.mockk

class UserCardinalGetServiceTest :
    DescribeSpec({

        val userCardinalRepository = mockk<UserCardinalRepository>()
        val userCardinalGetService = UserCardinalGetService(userCardinalRepository)

        describe("notContains") {
            it("유저의 기수 목록 중 특정 기수가 없으면 true를 반환한다") {
                val user = UserTestFixture.createActiveUser1()
                val existingCardinal = CardinalTestFixture.createCardinal(cardinalNumber = 7, year = 2025, semester = 2)
                val targetCardinal = CardinalTestFixture.createCardinal(cardinalNumber = 8, year = 2026, semester = 1)
                val userCardinal = UserCardinal(user, existingCardinal)

                every {
                    userCardinalRepository.findAllByUserOrderByCardinalCardinalNumberDesc(user)
                } returns listOf(userCardinal)

                val result = userCardinalGetService.notContains(user, targetCardinal)

                result.shouldBeTrue()
            }
        }

        describe("isCurrent") {
            context("현재 유저의 최신 기수보다 최신 기수일 때") {
                it("true를 반환한다") {
                    val user = UserTestFixture.createActiveUser1()
                    val oldCardinal = CardinalTestFixture.createCardinal(cardinalNumber = 7, year = 2025, semester = 2)
                    val newCardinal = CardinalTestFixture.createCardinal(cardinalNumber = 8, year = 2026, semester = 1)
                    val userCardinal = UserCardinal(user, oldCardinal)

                    every {
                        userCardinalRepository.findAllByUserOrderByCardinalCardinalNumberDesc(user)
                    } returns listOf(userCardinal)

                    val result = userCardinalGetService.isCurrent(user, newCardinal)

                    result.shouldBeTrue()
                }
            }

            context("새 기수가 기존 최대보다 작을 때") {
                it("false를 반환한다") {
                    val user = UserTestFixture.createActiveUser1()
                    val oldCardinal = CardinalTestFixture.createCardinal(cardinalNumber = 7, year = 2025, semester = 1)
                    val newCardinal = CardinalTestFixture.createCardinal(cardinalNumber = 6, year = 2024, semester = 2)
                    val userCardinal = UserCardinal(user, oldCardinal)

                    every {
                        userCardinalRepository.findAllByUserOrderByCardinalCardinalNumberDesc(user)
                    } returns listOf(userCardinal)

                    val result = userCardinalGetService.isCurrent(user, newCardinal)

                    result.shouldBeFalse()
                }
            }

            context("유저가 어떤 기수도 가지고 있지 않을 때") {
                it("CardinalNotFoundException이 발생한다") {
                    val user = UserTestFixture.createActiveUser1()
                    val newCardinal = CardinalTestFixture.createCardinal(cardinalNumber = 8, year = 2026, semester = 1)

                    every {
                        userCardinalRepository.findAllByUserOrderByCardinalCardinalNumberDesc(user)
                    } returns listOf()

                    shouldThrow<CardinalNotFoundException> {
                        userCardinalGetService.isCurrent(user, newCardinal)
                    }
                }
            }
        }
    })
