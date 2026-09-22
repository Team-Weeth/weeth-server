package com.weeth.domain.penalty.application.usecase.command

import com.weeth.domain.club.domain.repository.ClubRepository
import com.weeth.domain.club.domain.service.ClubPermissionPolicy
import com.weeth.domain.club.fixture.ClubMemberTestFixture
import com.weeth.domain.club.fixture.ClubTestFixture
import com.weeth.domain.penalty.application.dto.request.SavePenaltyRuleRequest
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk

class SavePenaltyRuleUseCaseTest :
    DescribeSpec({
        val clubRepository = mockk<ClubRepository>()
        val clubPermissionPolicy = mockk<ClubPermissionPolicy>()
        val useCase = SavePenaltyRuleUseCase(clubRepository, clubPermissionPolicy)

        beforeTest {
            clearMocks(clubRepository, clubPermissionPolicy)
        }

        describe("save") {
            it("페널티 규정을 저장한다") {
                val club = ClubTestFixture.createClub()
                val clubMember = ClubMemberTestFixture.createActiveMember(club = club)
                every { clubPermissionPolicy.requireAdmin(club.id, 1L) } returns clubMember
                every { clubRepository.getClubById(club.id) } returns club

                val ruleContent = "1. 정기모임 무단 불참: 5점\n2. 지각: 2점"
                useCase.save(
                    clubId = club.id,
                    userId = 1L,
                    request = SavePenaltyRuleRequest(content = ruleContent),
                )

                club.penaltyRule shouldBe ruleContent
            }

            it("빈 문자열로 페널티 규정을 초기화한다") {
                val club = ClubTestFixture.createClub()
                val clubMember = ClubMemberTestFixture.createActiveMember(club = club)
                every { clubPermissionPolicy.requireAdmin(club.id, 1L) } returns clubMember
                every { clubRepository.getClubById(club.id) } returns club

                useCase.save(
                    clubId = club.id,
                    userId = 1L,
                    request = SavePenaltyRuleRequest(content = ""),
                )

                club.penaltyRule shouldBe null
            }

            it("긴 텍스트의 페널티 규정을 저장한다") {
                val club = ClubTestFixture.createClub()
                val clubMember = ClubMemberTestFixture.createActiveMember(club = club)
                every { clubPermissionPolicy.requireAdmin(club.id, 1L) } returns clubMember
                every { clubRepository.getClubById(club.id) } returns club

                val longContent =
                    """
                    페널티 규정

                    1. 정기모임 무단 불참: 5점
                    2. 지각: 2점
                    3. 준비물 미준비: 3점

                    합계: 10점 이상 시 페널티
                    """.trimIndent()
                useCase.save(
                    clubId = club.id,
                    userId = 1L,
                    request = SavePenaltyRuleRequest(content = longContent),
                )

                club.penaltyRule shouldBe longContent
            }

            it("페널티 규정을 여러 번 수정한다") {
                val club = ClubTestFixture.createClub()
                val clubMember = ClubMemberTestFixture.createActiveMember(club = club)
                every { clubPermissionPolicy.requireAdmin(club.id, 1L) } returns clubMember
                every { clubRepository.getClubById(club.id) } returns club

                useCase.save(club.id, 1L, SavePenaltyRuleRequest("첫 번째 규정"))
                club.penaltyRule shouldBe "첫 번째 규정"

                useCase.save(club.id, 1L, SavePenaltyRuleRequest("두 번째 규정"))
                club.penaltyRule shouldBe "두 번째 규정"
            }
        }
    })
