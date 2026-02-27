package com.weeth.domain.user.application.usecase.command

import com.weeth.domain.user.application.dto.request.CardinalSaveRequest
import com.weeth.domain.user.application.dto.request.CardinalUpdateRequest
import com.weeth.domain.user.application.dto.response.CardinalResponse
import com.weeth.domain.user.application.mapper.CardinalMapper
import com.weeth.domain.user.application.usecase.query.GetCardinalQueryService
import com.weeth.domain.user.domain.entity.Cardinal
import com.weeth.domain.user.domain.entity.enums.CardinalStatus
import com.weeth.domain.user.domain.repository.CardinalRepository
import com.weeth.domain.user.fixture.CardinalTestFixture
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime
import java.util.Optional

class CardinalUseCaseTest :
    DescribeSpec({
        val cardinalRepository = mockk<CardinalRepository>()
        val cardinalMapper = mockk<CardinalMapper>()
        val manageCardinalUseCase = ManageCardinalUseCase(cardinalRepository, cardinalMapper)
        val getCardinalQueryService = GetCardinalQueryService(cardinalRepository, cardinalMapper)

        describe("save") {
            context("진행중이 아닌 기수라면") {
                it("검증 후 저장만 한다") {
                    val request = CardinalSaveRequest(7, 2025, 1, false)
                    val toSave = CardinalTestFixture.createCardinal(cardinalNumber = 7, year = 2025, semester = 1)
                    val saved = CardinalTestFixture.createCardinal(cardinalNumber = 7, year = 2025, semester = 1)

                    every { cardinalRepository.findByCardinalNumber(7) } returns Optional.empty()
                    every { cardinalMapper.toEntity(request) } returns toSave
                    every { cardinalRepository.save(toSave) } returns saved

                    manageCardinalUseCase.save(request)

                    verify { cardinalRepository.findByCardinalNumber(7) }
                    verify { cardinalRepository.save(toSave) }
                    verify(exactly = 0) { cardinalRepository.findAllByStatus(CardinalStatus.IN_PROGRESS) }
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

                    every { cardinalRepository.findByCardinalNumber(7) } returns Optional.empty()
                    every { cardinalRepository.findAllByStatus(CardinalStatus.IN_PROGRESS) } returns listOf(oldCardinal)
                    every { cardinalMapper.toEntity(request) } returns newCardinalBeforeSave
                    every { cardinalRepository.save(newCardinalBeforeSave) } returns newCardinalAfterSave

                    manageCardinalUseCase.save(request)

                    verify { cardinalRepository.findAllByStatus(CardinalStatus.IN_PROGRESS) }
                    verify { cardinalRepository.save(newCardinalBeforeSave) }

                    oldCardinal.status shouldBe CardinalStatus.DONE
                    newCardinalAfterSave.status shouldBe CardinalStatus.IN_PROGRESS
                }
            }
        }

        describe("update") {
            it("연도와 학기를 변경한다") {
                val cardinal = CardinalTestFixture.createCardinal(cardinalNumber = 6, year = 2024, semester = 2)
                every { cardinalRepository.findById(1L) } returns Optional.of(cardinal)

                manageCardinalUseCase.update(CardinalUpdateRequest(1L, 2025, 1, false))

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

                every { cardinalRepository.findAllByOrderByCardinalNumberAsc() } returns cardinals
                every { cardinalMapper.toResponse(cardinal1) } returns response1
                every { cardinalMapper.toResponse(cardinal2) } returns response2

                val responses = getCardinalQueryService.findAll()

                verify { cardinalRepository.findAllByOrderByCardinalNumberAsc() }
                verify(exactly = 2) { cardinalMapper.toResponse(any<Cardinal>()) }

                responses shouldHaveSize 2
                responses.map { it.cardinalNumber } shouldBe listOf(6, 7)
                responses.map { it.status } shouldBe listOf(CardinalStatus.DONE, CardinalStatus.IN_PROGRESS)
            }
        }
    })
