package com.weeth.domain.club.application.usecase.query

import com.weeth.domain.club.application.mapper.ClubPositionOptionMapper
import com.weeth.domain.club.domain.repository.ClubPositionOptionReader
import com.weeth.domain.club.domain.service.ClubPermissionPolicy
import com.weeth.domain.club.fixture.ClubMemberTestFixture
import com.weeth.domain.club.fixture.ClubPositionOptionTestFixture
import com.weeth.domain.club.fixture.ClubTestFixture
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class GetClubPositionOptionQueryServiceTest :
    DescribeSpec({
        val clubPositionOptionReader = mockk<ClubPositionOptionReader>()
        val clubPermissionPolicy = mockk<ClubPermissionPolicy>()
        val clubPositionOptionMapper = ClubPositionOptionMapper()
        val queryService =
            GetClubPositionOptionQueryService(
                clubPositionOptionReader = clubPositionOptionReader,
                clubPermissionPolicy = clubPermissionPolicy,
                clubPositionOptionMapper = clubPositionOptionMapper,
            )

        val club = ClubTestFixture.createClub(id = 1L)
        val adminMember = ClubMemberTestFixture.createAdminMember(club = club)

        beforeTest {
            clearMocks(clubPositionOptionReader, clubPermissionPolicy)
            every { clubPermissionPolicy.requireAdmin(1L, 10L) } returns adminMember
        }

        describe("findAll") {
            it("displayOrder 오름차순으로 조회된 옵션을 응답 DTO로 변환한다") {
                val optionA =
                    ClubPositionOptionTestFixture.createOption(
                        id = 1L,
                        club = club,
                        name = "백엔드",
                        displayOrder = 0,
                    )
                val optionB =
                    ClubPositionOptionTestFixture.createOption(
                        id = 2L,
                        club = club,
                        name = "프론트엔드",
                        displayOrder = 1,
                    )
                every { clubPositionOptionReader.findAllByClubIdOrderByDisplayOrderAsc(1L) } returns
                    listOf(optionA, optionB)

                val responses = queryService.findAll(1L, 10L)

                responses.size shouldBe 2
                responses[0].name shouldBe "백엔드"
                responses[0].displayOrder shouldBe 0
                responses[1].name shouldBe "프론트엔드"
                responses[1].displayOrder shouldBe 1
            }

            it("저장된 옵션이 없으면 빈 리스트를 반환한다") {
                every { clubPositionOptionReader.findAllByClubIdOrderByDisplayOrderAsc(1L) } returns emptyList()

                val responses = queryService.findAll(1L, 10L)

                responses shouldBe emptyList()
            }

            it("관리자 권한을 먼저 검증한다") {
                every { clubPositionOptionReader.findAllByClubIdOrderByDisplayOrderAsc(1L) } returns emptyList()

                queryService.findAll(1L, 10L)

                verify(exactly = 1) { clubPermissionPolicy.requireAdmin(1L, 10L) }
            }
        }
    })
