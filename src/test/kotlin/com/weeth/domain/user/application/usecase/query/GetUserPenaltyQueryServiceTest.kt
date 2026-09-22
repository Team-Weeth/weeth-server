package com.weeth.domain.user.application.usecase.query

import com.weeth.domain.cardinal.domain.repository.CardinalReader
import com.weeth.domain.cardinal.fixture.CardinalTestFixture
import com.weeth.domain.club.domain.repository.ClubMemberCardinalReader
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.club.fixture.ClubMemberCardinalTestFixture
import com.weeth.domain.club.fixture.ClubMemberTestFixture
import com.weeth.domain.club.fixture.ClubTestFixture
import com.weeth.domain.penalty.domain.enums.PenaltyType
import com.weeth.domain.penalty.domain.repository.PenaltyReader
import com.weeth.domain.penalty.fixture.PenaltyTestFixture
import com.weeth.domain.user.application.exception.UserPageNotFoundException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.springframework.data.domain.SliceImpl
import org.springframework.test.util.ReflectionTestUtils

class GetUserPenaltyQueryServiceTest :
    DescribeSpec({
        val penaltyReader = mockk<PenaltyReader>()
        val clubMemberPolicy = mockk<ClubMemberPolicy>()
        val clubMemberCardinalReader = mockk<ClubMemberCardinalReader>()
        val cardinalReader = mockk<CardinalReader>()
        val service =
            GetUserPenaltyQueryService(
                penaltyReader = penaltyReader,
                clubMemberPolicy = clubMemberPolicy,
                clubMemberCardinalReader = clubMemberCardinalReader,
                cardinalReader = cardinalReader,
            )

        beforeTest {
            clearMocks(penaltyReader, clubMemberPolicy, clubMemberCardinalReader, cardinalReader)
        }

        describe("getMyPenalties") {
            it("페이지 번호가 음수면 예외를 던진다") {
                shouldThrow<UserPageNotFoundException> {
                    service.getMyPenalties(1L, 1L, null, -1, 5)
                }
            }

            it("페이지 크기가 범위를 벗어나면 예외를 던진다") {
                shouldThrow<UserPageNotFoundException> {
                    service.getMyPenalties(1L, 1L, null, 0, 0)
                }
            }

            it("기수 필터 없으면 전체 기수 대상으로 penaltyType별 총 횟수를 계산한다") {
                val club = ClubTestFixture.createClub()
                ReflectionTestUtils.setField(club, "warningEnabled", true)
                val clubMember = ClubMemberTestFixture.createActiveMember(club = club)
                val cardinal = CardinalTestFixture.createCardinal(club = club, cardinalNumber = 7)
                val memberCardinal = ClubMemberCardinalTestFixture.create(clubMember = clubMember, cardinal = cardinal)
                val penalty = PenaltyTestFixture.createPenalty(clubMember = clubMember, cardinal = cardinal)

                every { clubMemberPolicy.getActiveMember(1L, 1L) } returns clubMember
                every { clubMemberCardinalReader.findAllByClubMember(clubMember) } returns listOf(memberCardinal)
                every {
                    penaltyReader.findSliceByClubMemberId(clubMember.id, null, any())
                } returns SliceImpl(listOf(penalty))
                every {
                    penaltyReader.countByClubMemberIdAndCardinalIdAndPenaltyType(
                        clubMember.id,
                        null,
                        PenaltyType.PENALTY,
                    )
                } returns 3
                every {
                    penaltyReader.countByClubMemberIdAndCardinalIdAndPenaltyType(
                        clubMember.id,
                        null,
                        PenaltyType.WARNING,
                    )
                } returns 2

                val result = service.getMyPenalties(1L, 1L, null, 0, 5)

                result.penaltyCount shouldBe 3
                result.warningCount shouldBe 2
                result.cardinals shouldBe listOf(7)
                result.penalties.content shouldBe
                    listOf(
                        com.weeth.domain.user.application.dto.response.UserMyPenaltyResponse(
                            penaltyId = penalty.id,
                            penaltyDescription = penalty.penaltyDescription,
                            penaltyType = penalty.penaltyType,
                            createdAt = penalty.createdAt,
                        ),
                    )
            }

            it("동아리 경고 기능이 비활성화면 warningCount는 null이다") {
                val club = ClubTestFixture.createClub()
                val clubMember = ClubMemberTestFixture.createActiveMember(club = club)

                every { clubMemberPolicy.getActiveMember(any(), any()) } returns clubMember
                every { clubMemberCardinalReader.findAllByClubMember(clubMember) } returns emptyList()
                every {
                    penaltyReader.findSliceByClubMemberId(clubMember.id, null, any())
                } returns SliceImpl(emptyList())
                every {
                    penaltyReader.countByClubMemberIdAndCardinalIdAndPenaltyType(
                        clubMember.id,
                        null,
                        PenaltyType.PENALTY,
                    )
                } returns 0

                val result = service.getMyPenalties(1L, 1L, null, 0, 5)

                result.warningCount shouldBe null
            }

            it("cardinalNumber를 지정하면 해당 기수의 cardinalId로 조회한다") {
                val club = ClubTestFixture.createClub()
                val clubMember = ClubMemberTestFixture.createActiveMember(club = club)
                val cardinal = CardinalTestFixture.createCardinal(id = 42L, club = club, cardinalNumber = 6)

                every { clubMemberPolicy.getActiveMember(any(), any()) } returns clubMember
                every { clubMemberCardinalReader.findAllByClubMember(clubMember) } returns emptyList()
                every { cardinalReader.findByClubIdAndCardinalNumber(clubMember.club.id, 6) } returns cardinal
                every {
                    penaltyReader.findSliceByClubMemberId(clubMember.id, 42L, any())
                } returns SliceImpl(emptyList())
                every {
                    penaltyReader.countByClubMemberIdAndCardinalIdAndPenaltyType(
                        clubMember.id,
                        42L,
                        PenaltyType.PENALTY,
                    )
                } returns 1

                val result = service.getMyPenalties(1L, clubMember.club.id, 6, 0, 5)

                result.penaltyCount shouldBe 1
            }

            it("존재하지 않는 기수를 지정하면 빈 결과와 0 카운트를 반환한다") {
                val club = ClubTestFixture.createClub()
                ReflectionTestUtils.setField(club, "warningEnabled", true)
                val clubMember = ClubMemberTestFixture.createActiveMember(club = club)

                every { clubMemberPolicy.getActiveMember(any(), any()) } returns clubMember
                every { clubMemberCardinalReader.findAllByClubMember(clubMember) } returns emptyList()
                every { cardinalReader.findByClubIdAndCardinalNumber(clubMember.club.id, 999) } returns null

                val result = service.getMyPenalties(1L, clubMember.club.id, 999, 0, 5)

                result.penaltyCount shouldBe 0
                result.warningCount shouldBe 0
                result.penalties.content shouldBe emptyList()
            }
        }
    })
