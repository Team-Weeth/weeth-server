package com.weeth.domain.cardinal.application.usecase.command

import com.weeth.domain.cardinal.application.dto.request.CardinalSaveRequest
import com.weeth.domain.cardinal.application.dto.response.CardinalResponse
import com.weeth.domain.cardinal.application.mapper.CardinalMapper
import com.weeth.domain.cardinal.application.usecase.query.GetCardinalQueryService
import com.weeth.domain.cardinal.domain.entity.Cardinal
import com.weeth.domain.cardinal.domain.enums.CardinalStatus
import com.weeth.domain.cardinal.domain.repository.CardinalReader
import com.weeth.domain.cardinal.domain.repository.CardinalRepository
import com.weeth.domain.cardinal.domain.service.CardinalStatusPolicy
import com.weeth.domain.cardinal.fixture.CardinalTestFixture
import com.weeth.domain.club.domain.repository.ClubReader
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.club.domain.service.ClubPermissionPolicy
import com.weeth.domain.club.fixture.ClubTestFixture
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime

class CardinalUseCaseTest :
    DescribeSpec({
        val cardinalRepository = mockk<CardinalRepository>()
        val cardinalReader = mockk<CardinalReader>()
        val cardinalMapper = mockk<CardinalMapper>()
        val clubReader = mockk<ClubReader>()
        val clubMemberPolicy = mockk<ClubMemberPolicy>(relaxed = true)
        val clubPermissionPolicy = mockk<ClubPermissionPolicy>(relaxed = true)
        val cardinalStatusPolicy = CardinalStatusPolicy(cardinalRepository)
        val manageCardinalUseCase =
            ManageCardinalUseCase(
                cardinalRepository,
                cardinalMapper,
                cardinalStatusPolicy,
                clubReader,
                clubPermissionPolicy,
            )
        val getCardinalQueryService =
            GetCardinalQueryService(cardinalReader, clubMemberPolicy, cardinalMapper)

        val clubId = 1L
        val userId = 99L
        val club = ClubTestFixture.createClub()

        beforeTest {
            clearMocks(
                cardinalRepository,
                cardinalReader,
                cardinalMapper,
                clubReader,
                clubMemberPolicy,
                clubPermissionPolicy,
            )
            every { clubReader.getClubById(clubId) } returns club
            every {
                clubMemberPolicy.getActiveMember(
                    clubId,
                    userId,
                )
            } returns ClubTestFixture.createClubMember(club = club)
        }

        describe("save") {
            context("진행중이 아닌 기수라면") {
                it("검증 후 저장만 한다") {
                    val request = CardinalSaveRequest(7, false)
                    val toSave = CardinalTestFixture.createCardinal(cardinalNumber = 7)
                    val saved = CardinalTestFixture.createCardinal(cardinalNumber = 7)

                    every { cardinalRepository.findByClubIdAndCardinalNumber(clubId, 7) } returns null
                    every { cardinalMapper.toEntity(club, request) } returns toSave
                    every { cardinalRepository.save(toSave) } returns saved

                    manageCardinalUseCase.save(clubId, request, userId)

                    verify { cardinalRepository.findByClubIdAndCardinalNumber(clubId, 7) }
                    verify { cardinalRepository.save(toSave) }
                    verify(exactly = 0) { cardinalRepository.findAllInProgressWithLock() }
                }
            }

            context("새 기수가 진행중이라면") {
                it("기존 기수는 DONE, 현재기수는 IN_PROGRESS가 된다") {
                    val request = CardinalSaveRequest(7, true)
                    val oldCardinal = CardinalTestFixture.createCardinalInProgress(cardinalNumber = 6)
                    val newCardinalBeforeSave = CardinalTestFixture.createCardinal(cardinalNumber = 7)
                    val newCardinalAfterSave = CardinalTestFixture.createCardinal(cardinalNumber = 7)

                    every { cardinalRepository.findByClubIdAndCardinalNumber(clubId, 7) } returns null
                    every { cardinalRepository.findAllInProgressWithLock() } returns listOf(oldCardinal)
                    every { cardinalMapper.toEntity(club, request) } returns newCardinalBeforeSave
                    every { cardinalRepository.save(newCardinalBeforeSave) } returns newCardinalAfterSave

                    manageCardinalUseCase.save(clubId, request, userId)

                    verify { cardinalRepository.findAllInProgressWithLock() }
                    verify { cardinalRepository.save(newCardinalBeforeSave) }

                    oldCardinal.status shouldBe CardinalStatus.DONE
                    newCardinalAfterSave.status shouldBe CardinalStatus.IN_PROGRESS
                }
            }
        }

        describe("activate") {
            it("해당 기수를 IN_PROGRESS로 지정하고 나머지는 DONE으로 변경한다") {
                val cardinal = CardinalTestFixture.createCardinal(cardinalNumber = 6)
                val oldCardinal = CardinalTestFixture.createCardinalInProgress(cardinalNumber = 5)
                every { cardinalRepository.findByIdAndClubId(1L, clubId) } returns cardinal
                every { cardinalRepository.findAllInProgressWithLock() } returns listOf(oldCardinal)

                manageCardinalUseCase.activate(clubId, 1L, userId)

                oldCardinal.status shouldBe CardinalStatus.DONE
                cardinal.status shouldBe CardinalStatus.IN_PROGRESS
            }
        }

        describe("findAll") {
            it("조회된 모든 기수를 DTO로 매핑한다") {
                val cardinal1 = CardinalTestFixture.createCardinal(id = 1L, cardinalNumber = 6)
                val cardinal2 = CardinalTestFixture.createCardinalInProgress(id = 2L, cardinalNumber = 7)
                val cardinals = listOf(cardinal1, cardinal2)
                val now = LocalDateTime.now()

                val response1 = CardinalResponse(1L, 6, CardinalStatus.DONE, now.minusDays(5), now.minusDays(3))
                val response2 = CardinalResponse(2L, 7, CardinalStatus.IN_PROGRESS, now.minusDays(2), now)

                every { cardinalReader.findAllByClubIdOrderByCardinalNumberAsc(clubId) } returns cardinals
                every { cardinalMapper.toResponse(cardinal1) } returns response1
                every { cardinalMapper.toResponse(cardinal2) } returns response2

                val responses = getCardinalQueryService.findAll(clubId, userId)

                verify { cardinalReader.findAllByClubIdOrderByCardinalNumberAsc(clubId) }
                verify(exactly = 2) { cardinalMapper.toResponse(any<Cardinal>()) }

                responses shouldHaveSize 2
                responses.map { it.cardinalNumber } shouldBe listOf(6, 7)
                responses.map { it.status } shouldBe listOf(CardinalStatus.DONE, CardinalStatus.IN_PROGRESS)
            }
        }
    })
