package com.weeth.domain.user.application.usecase

import com.weeth.domain.user.application.dto.request.CardinalSaveRequest
import com.weeth.domain.user.application.dto.request.CardinalUpdateRequest
import com.weeth.domain.user.application.dto.response.CardinalResponse
import com.weeth.domain.user.application.mapper.CardinalMapper
import com.weeth.domain.user.domain.entity.Cardinal
import com.weeth.domain.user.domain.entity.enums.CardinalStatus
import com.weeth.domain.user.domain.service.CardinalGetService
import com.weeth.domain.user.domain.service.CardinalSaveService
import com.weeth.domain.user.fixture.CardinalTestFixture
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime

class CardinalUseCaseTest :
    DescribeSpec({

        val cardinalGetService = mockk<CardinalGetService>()
        val cardinalSaveService = mockk<CardinalSaveService>()
        val cardinalMapper = mockk<CardinalMapper>()
        val useCase = CardinalUseCase(cardinalGetService, cardinalSaveService, cardinalMapper)

        describe("save") {
            context("진행중이 아닌 기수라면") {
                it("검증 후 저장만 한다") {
                    val request = CardinalSaveRequest(7, 2025, 1, false)
                    val toSave = CardinalTestFixture.createCardinal(cardinalNumber = 7, year = 2025, semester = 1)
                    val saved = CardinalTestFixture.createCardinal(cardinalNumber = 7, year = 2025, semester = 1)

                    every { cardinalGetService.validateCardinal(7) } just Runs
                    every { cardinalMapper.from(request) } returns toSave
                    every { cardinalSaveService.save(toSave) } returns saved

                    useCase.save(request)

                    verify { cardinalGetService.validateCardinal(7) }
                    verify { cardinalSaveService.save(toSave) }
                    verify(exactly = 0) { cardinalGetService.findInProgress() }
                }
            }

            context("새 기수가 진행중이라면") {
                it("기존 기수는 DONE, 현재기수는 IN_PROGRESS가 된다") {
                    val request = CardinalSaveRequest(7, 2025, 1, true)
                    val oldCardinal =
                        CardinalTestFixture.createCardinalInProgress(cardinalNumber = 6, year = 2024, semester = 2)
                    val newCardinalBeforeSave =
                        CardinalTestFixture.createCardinal(cardinalNumber = 7, year = 2025, semester = 1)
                    val newCardinalAfterSave =
                        CardinalTestFixture.createCardinal(cardinalNumber = 7, year = 2025, semester = 1)

                    every { cardinalGetService.validateCardinal(7) } just Runs
                    every { cardinalGetService.findInProgress() } returns listOf(oldCardinal)
                    every { cardinalMapper.from(request) } returns newCardinalBeforeSave
                    every { cardinalSaveService.save(newCardinalBeforeSave) } returns newCardinalAfterSave

                    useCase.save(request)

                    verify { cardinalGetService.findInProgress() }
                    verify { cardinalSaveService.save(newCardinalBeforeSave) }

                    oldCardinal.status shouldBe CardinalStatus.DONE
                    newCardinalAfterSave.status shouldBe CardinalStatus.IN_PROGRESS
                }
            }
        }

        describe("update") {
            it("연도와 학기를 변경한다") {
                val cardinal = CardinalTestFixture.createCardinal(cardinalNumber = 6, year = 2024, semester = 2)
                val dto = CardinalUpdateRequest(1L, 2025, 1, false)

                cardinal.update(dto)

                cardinal.year shouldBe 2025
                cardinal.semester shouldBe 1
            }
        }

        describe("findAll") {
            it("조회된 모든 기수를 DTO로 매핑한다") {
                val cardinal1 =
                    CardinalTestFixture.createCardinal(id = 1L, cardinalNumber = 6, year = 2024, semester = 2)
                val cardinal2 =
                    CardinalTestFixture.createCardinalInProgress(id = 2L, cardinalNumber = 7, year = 2025, semester = 1)
                val cardinals = listOf(cardinal1, cardinal2)
                val now = LocalDateTime.now()

                val response1 = CardinalResponse(1L, 6, 2024, 2, CardinalStatus.DONE, now.minusDays(5), now.minusDays(3))
                val response2 =
                    CardinalResponse(2L, 7, 2025, 1, CardinalStatus.IN_PROGRESS, now.minusDays(2), now)

                every { cardinalGetService.findAll() } returns cardinals
                every { cardinalMapper.to(cardinal1) } returns response1
                every { cardinalMapper.to(cardinal2) } returns response2

                val responses = useCase.findAll()

                verify { cardinalGetService.findAll() }
                verify(exactly = 2) { cardinalMapper.to(any<Cardinal>()) }

                responses shouldHaveSize 2
                responses.map { it.cardinalNumber() } shouldBe listOf(6, 7)
                responses.map { it.status() } shouldBe listOf(CardinalStatus.DONE, CardinalStatus.IN_PROGRESS)
            }
        }
    })
