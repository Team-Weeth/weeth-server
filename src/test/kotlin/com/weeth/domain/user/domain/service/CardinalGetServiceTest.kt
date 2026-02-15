package com.weeth.domain.user.domain.service

import com.weeth.domain.user.application.exception.DuplicateCardinalException
import com.weeth.domain.user.domain.entity.Cardinal
import com.weeth.domain.user.domain.repository.CardinalRepository
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.util.Optional

class CardinalGetServiceTest :
    DescribeSpec({

        val cardinalRepository = mockk<CardinalRepository>()
        val cardinalGetService = CardinalGetService(cardinalRepository)

        describe("findByAdminSide") {
            context("존재하지 않는 기수를 넣었을 때") {
                it("새로 저장된다") {
                    every { cardinalRepository.findByCardinalNumber(7) } returns Optional.empty()
                    every { cardinalRepository.save(any<Cardinal>()) } returns
                        Cardinal.builder().cardinalNumber(7).build()

                    val result = cardinalGetService.findByAdminSide(7)

                    result.cardinalNumber shouldBe 7
                }
            }
        }

        describe("validateCardinal") {
            context("중복된 기수일 때") {
                it("예외를 던진다") {
                    every { cardinalRepository.findByCardinalNumber(7) } returns
                        Optional.of(Cardinal.builder().cardinalNumber(7).build())

                    shouldThrow<DuplicateCardinalException> {
                        cardinalGetService.validateCardinal(7)
                    }
                }
            }

            context("중복되지 않는 기수일 때") {
                it("예외를 던지지 않는다") {
                    every { cardinalRepository.findByCardinalNumber(7) } returns Optional.empty()

                    shouldNotThrowAny {
                        cardinalGetService.validateCardinal(7)
                    }
                }
            }
        }
    })
