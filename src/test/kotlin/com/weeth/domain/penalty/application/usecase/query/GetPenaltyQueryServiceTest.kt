package com.weeth.domain.penalty.application.usecase.query

import com.weeth.domain.cardinal.domain.repository.CardinalReader
import com.weeth.domain.cardinal.fixture.CardinalTestFixture
import com.weeth.domain.club.application.exception.ClubMemberNotFoundException
import com.weeth.domain.club.application.exception.ClubMemberNotInClubException
import com.weeth.domain.club.domain.repository.ClubMemberCardinalReader
import com.weeth.domain.club.domain.repository.ClubMemberReader
import com.weeth.domain.club.domain.service.ClubMemberCardinalPolicy
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.club.domain.service.ClubPermissionPolicy
import com.weeth.domain.club.fixture.ClubMemberCardinalTestFixture
import com.weeth.domain.club.fixture.ClubMemberTestFixture
import com.weeth.domain.club.fixture.ClubTestFixture
import com.weeth.domain.penalty.application.mapper.PenaltyMapper
import com.weeth.domain.penalty.domain.repository.PenaltyRepository
import com.weeth.domain.penalty.fixture.PenaltyTestFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk

class GetPenaltyQueryServiceTest :
    DescribeSpec({
        val penaltyRepository = mockk<PenaltyRepository>()
        val clubMemberCardinalReader = mockk<ClubMemberCardinalReader>()
        val clubMemberReader = mockk<ClubMemberReader>()
        val clubMemberPolicy = mockk<ClubMemberPolicy>()
        val clubPermissionPolicy = mockk<ClubPermissionPolicy>()
        val clubMemberCardinalPolicy = mockk<ClubMemberCardinalPolicy>()
        val cardinalReader = mockk<CardinalReader>()
        val mapper = mockk<PenaltyMapper>()

        val queryService =
            GetPenaltyQueryService(
                penaltyRepository = penaltyRepository,
                clubMemberCardinalReader = clubMemberCardinalReader,
                clubMemberReader = clubMemberReader,
                clubMemberPolicy = clubMemberPolicy,
                clubPermissionPolicy = clubPermissionPolicy,
                clubMemberCardinalPolicy = clubMemberCardinalPolicy,
                cardinalReader = cardinalReader,
                mapper = mapper,
            )

        beforeTest {
            clearMocks(
                penaltyRepository,
                clubMemberCardinalReader,
                clubMemberReader,
                clubMemberPolicy,
                clubPermissionPolicy,
                clubMemberCardinalPolicy,
                cardinalReader,
                mapper,
            )
        }

        describe("findAllByCardinal") {
            it("특정 기수의 모든 페널티를 조회한다") {
                val cardinal = CardinalTestFixture.createCardinal(cardinalNumber = 1)
                val clubMember = ClubMemberTestFixture.createActiveMember(club = cardinal.club)
                val penalty = PenaltyTestFixture.createPenalty(clubMember = clubMember, cardinal = cardinal)
                val clubMemberCardinal =
                    ClubMemberCardinalTestFixture.create(clubMember = clubMember, cardinal = cardinal)

                every { clubPermissionPolicy.requireAdmin(cardinal.club.id, 1L) } returns clubMember
                every { cardinalReader.findByClubIdAndCardinalNumber(cardinal.club.id, 1) } returns cardinal
                every {
                    penaltyRepository.findByClubIdAndCardinalIdOrderByIdDesc(
                        cardinal.club.id,
                        cardinal.id,
                    )
                } returns
                    listOf(penalty)
                every { clubMemberCardinalReader.findAllByClubMembers(any()) } returns listOf(clubMemberCardinal)
                every { mapper.toResponse(any(), any(), any()) } returns mockk()
                every { mapper.toByCardinalResponse(any(), any()) } returns mockk()

                val result =
                    queryService.findAllByCardinal(
                        clubId = cardinal.club.id,
                        userId = 1L,
                        cardinalNumber = 1,
                    )

                result shouldHaveSize 1
            }

            it("기수 번호가 없으면 모든 기수의 페널티를 조회한다") {
                val club = ClubTestFixture.createClub()
                val cardinal1 = CardinalTestFixture.createCardinal(cardinalNumber = 1, club = club)
                val cardinal2 = CardinalTestFixture.createCardinal(cardinalNumber = 2, club = club)
                val clubMember = ClubMemberTestFixture.createActiveMember(club = club)
                val penalty1 = PenaltyTestFixture.createPenalty(clubMember = clubMember, cardinal = cardinal1)
                val penalty2 = PenaltyTestFixture.createPenalty(clubMember = clubMember, cardinal = cardinal2)

                every { clubPermissionPolicy.requireAdmin(club.id, 1L) } returns clubMember
                every { cardinalReader.findAllByClubIdOrderByCardinalNumberAsc(cardinal1.club.id) } returns
                    listOf(cardinal1, cardinal2)
                every {
                    penaltyRepository.findByClubIdAndCardinalIdOrderByIdDesc(
                        cardinal1.club.id,
                        cardinal1.id,
                    )
                } returns
                    listOf(penalty1)
                every {
                    penaltyRepository.findByClubIdAndCardinalIdOrderByIdDesc(
                        cardinal1.club.id,
                        cardinal2.id,
                    )
                } returns
                    listOf(penalty2)
                every { clubMemberCardinalReader.findAllByClubMembers(any()) } returns emptyList()
                every { mapper.toResponse(any(), any(), any()) } returns mockk()
                every { mapper.toByCardinalResponse(any(), any()) } returns mockk()

                val result =
                    queryService.findAllByCardinal(
                        clubId = cardinal1.club.id,
                        userId = 1L,
                        cardinalNumber = null,
                    )

                result shouldHaveSize 2
            }

            it("존재하지 않는 기수이면 빈 리스트를 반환한다") {
                val clubId = 1L
                val clubMember = ClubMemberTestFixture.createActiveMember()
                every { clubPermissionPolicy.requireAdmin(clubId, 1L) } returns clubMember
                every { cardinalReader.findByClubIdAndCardinalNumber(clubId, 999) } returns null

                val result =
                    queryService.findAllByCardinal(
                        clubId = clubId,
                        userId = 1L,
                        cardinalNumber = 999,
                    )

                result shouldHaveSize 0
            }

            it("관리자 권한이 없으면 예외를 던진다") {
                every { clubPermissionPolicy.requireAdmin(any(), any()) } throws RuntimeException()

                shouldThrow<RuntimeException> {
                    queryService.findAllByCardinal(clubId = 1L, userId = 1L, cardinalNumber = 1)
                }
            }
        }

        describe("findMemberPenaltyDetail") {
            it("멤버의 페널티 상세 정보를 조회한다") {
                val clubMember = ClubMemberTestFixture.createActiveMember()
                val cardinal = CardinalTestFixture.createCardinal(club = clubMember.club, cardinalNumber = 1)
                val penalty = PenaltyTestFixture.createPenalty(clubMember = clubMember, cardinal = cardinal)
                val clubMemberCardinal =
                    ClubMemberCardinalTestFixture.create(clubMember = clubMember, cardinal = cardinal)

                every { clubPermissionPolicy.requireAdmin(clubMember.club.id, 1L) } returns clubMember
                every { clubMemberReader.findAdminMemberDetail(clubMember.id) } returns clubMember
                every { clubMemberCardinalReader.findAllByClubMember(clubMember) } returns listOf(clubMemberCardinal)
                every { penaltyRepository.findByClubMemberIds(listOf(clubMember.id)) } returns listOf(penalty)
                every { mapper.toMemberPenaltyDetailResponse(any(), any(), any()) } returns mockk()

                queryService.findMemberPenaltyDetail(
                    clubId = clubMember.club.id,
                    userId = 1L,
                    clubMemberId = clubMember.id,
                )
            }

            it("존재하지 않는 멤버이면 예외를 던진다") {
                val clubMember = ClubMemberTestFixture.createActiveMember()
                every { clubPermissionPolicy.requireAdmin(clubMember.club.id, 1L) } returns clubMember
                every { clubMemberReader.findAdminMemberDetail(999L) } returns null

                shouldThrow<ClubMemberNotFoundException> {
                    queryService.findMemberPenaltyDetail(clubId = clubMember.club.id, userId = 1L, clubMemberId = 999L)
                }
            }

            it("다른 클럽의 멤버이면 예외를 던진다") {
                val clubMember = ClubMemberTestFixture.createActiveMember()
                every { clubPermissionPolicy.requireAdmin(999L, 1L) } returns clubMember
                every { clubMemberReader.findAdminMemberDetail(clubMember.id) } returns clubMember

                shouldThrow<ClubMemberNotInClubException> {
                    queryService.findMemberPenaltyDetail(clubId = 999L, userId = 1L, clubMemberId = clubMember.id)
                }
            }

            it("관리자 권한이 없으면 예외를 던진다") {
                every { clubPermissionPolicy.requireAdmin(any(), any()) } throws RuntimeException()

                shouldThrow<RuntimeException> {
                    queryService.findMemberPenaltyDetail(clubId = 1L, userId = 1L, clubMemberId = 1L)
                }
            }
        }

        describe("findByUser") {
            it("사용자의 현재 기수 페널티를 조회한다") {
                val clubMember = ClubMemberTestFixture.createActiveMember()
                val cardinal = CardinalTestFixture.createCardinal(club = clubMember.club, cardinalNumber = 1)
                val penalty = PenaltyTestFixture.createPenalty(clubMember = clubMember, cardinal = cardinal)
                val clubMemberCardinal =
                    ClubMemberCardinalTestFixture.create(clubMember = clubMember, cardinal = cardinal)

                every { clubMemberPolicy.getActiveMember(clubMember.club.id, clubMember.id) } returns clubMember
                every { clubMemberCardinalPolicy.getCurrentCardinal(clubMember) } returns cardinal
                every {
                    penaltyRepository.findByClubMemberIdAndCardinalIdOrderByIdDesc(
                        clubMember.id,
                        cardinal.id,
                    )
                } returns listOf(penalty)
                every { clubMemberCardinalReader.findAllByClubMember(clubMember) } returns listOf(clubMemberCardinal)
                every { mapper.toResponse(any(), any(), any()) } returns mockk()

                queryService.findByUser(clubMember.club.id, clubMember.id)
            }
        }
    })
