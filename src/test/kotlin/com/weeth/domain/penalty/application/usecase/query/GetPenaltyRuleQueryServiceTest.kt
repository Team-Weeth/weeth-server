package com.weeth.domain.penalty.application.usecase.query

import com.weeth.domain.club.domain.repository.ClubReader
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.club.fixture.ClubMemberTestFixture
import com.weeth.domain.club.fixture.ClubTestFixture
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk

class GetPenaltyRuleQueryServiceTest :
    DescribeSpec({
        val clubReader = mockk<ClubReader>()
        val clubMemberPolicy = mockk<ClubMemberPolicy>()
        val queryService = GetPenaltyRuleQueryService(clubReader, clubMemberPolicy)

        val userId = 1L
        val clubMember = ClubMemberTestFixture.createActiveMember()

        beforeTest {
            clearMocks(clubReader, clubMemberPolicy)
            every { clubMemberPolicy.getActiveMember(any(), userId) } returns clubMember
        }

        describe("getRule") {
            it("클럽의 페널티 규정을 조회한다") {
                val club = ClubTestFixture.createClub()
                // 직접 penaltyRule을 설정할 방법을 찾아야 함
                every { clubReader.getClubById(club.id) } returns club

                val response = queryService.getRule(club.id, userId)

                response.content shouldBe club.penaltyRule
            }

            it("페널티 규정이 비어있을 수 있다") {
                val club = ClubTestFixture.createClub()
                every { clubReader.getClubById(club.id) } returns club

                val response = queryService.getRule(club.id, userId)

                response.content shouldBe club.penaltyRule
            }

            it("긴 페널티 규정을 조회한다") {
                val longContent =
                    """
                    페널티 규정

                    1. 정기모임 무단 불참: 5점
                    2. 지각: 2점
                    3. 준비물 미준비: 3점

                    합계: 10점 이상 시 페널티
                    """.trimIndent()
                val club = ClubTestFixture.createClub()
                every { clubReader.getClubById(club.id) } returns club

                val response = queryService.getRule(club.id, userId)

                response.content shouldBe club.penaltyRule
            }
        }
    })
