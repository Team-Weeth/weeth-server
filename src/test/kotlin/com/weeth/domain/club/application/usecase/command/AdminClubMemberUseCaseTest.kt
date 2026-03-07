package com.weeth.domain.club.application.usecase.command

import com.weeth.domain.club.application.dto.request.ClubMemberRoleUpdateRequest
import com.weeth.domain.club.application.exception.ClubMemberNotInClubException
import com.weeth.domain.club.domain.enums.MemberRole
import com.weeth.domain.club.domain.enums.MemberStatus
import com.weeth.domain.club.domain.repository.ClubMemberRepository
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.club.fixture.ClubMemberTestFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class AdminClubMemberUseCaseTest :
    DescribeSpec({
        val clubMemberRepository = mockk<ClubMemberRepository>()
        val clubMemberPolicy = mockk<ClubMemberPolicy>()
        val useCase = AdminClubMemberUseCase(clubMemberRepository, clubMemberPolicy)
        val adminMember = ClubMemberTestFixture.createAdminMember()

        describe("accept") {
            it("같은 동아리 소속 멤버를 승인한다") {
                val member = ClubMemberTestFixture.createWaitingMember()
                every { clubMemberPolicy.requireAdmin(1L, 10L) } returns adminMember
                every { clubMemberPolicy.getMemberInClub(1L, 20L) } returns member

                useCase.accept(1L, 10L, 20L)

                member.memberStatus shouldBe MemberStatus.ACTIVE
                verify(exactly = 0) { clubMemberRepository.getClubMemberById(any()) }
            }

            it("다른 동아리 소속 멤버면 예외가 발생한다") {
                every { clubMemberPolicy.requireAdmin(1L, 10L) } returns adminMember
                every { clubMemberPolicy.getMemberInClub(1L, 20L) } throws ClubMemberNotInClubException()

                shouldThrow<ClubMemberNotInClubException> {
                    useCase.accept(1L, 10L, 20L)
                }
            }
        }

        describe("ban") {
            it("같은 동아리 소속 멤버를 추방한다") {
                val member = ClubMemberTestFixture.createActiveMember()
                every { clubMemberPolicy.requireAdmin(1L, 10L) } returns adminMember
                every { clubMemberPolicy.getMemberInClub(1L, 20L) } returns member

                useCase.ban(1L, 10L, 20L)

                member.memberStatus shouldBe MemberStatus.BANNED
            }
        }

        describe("updateMemberRole") {
            it("같은 동아리 소속 멤버의 권한을 변경한다") {
                val member = ClubMemberTestFixture.createActiveMember(memberRole = MemberRole.USER)
                every { clubMemberPolicy.requireAdmin(1L, 10L) } returns adminMember
                every { clubMemberPolicy.getMemberInClub(1L, 20L) } returns member

                useCase.updateMemberRole(
                    1L,
                    10L,
                    ClubMemberRoleUpdateRequest(clubMemberId = 20L, memberRole = MemberRole.ADMIN),
                )

                member.memberRole shouldBe MemberRole.ADMIN
            }
        }
    })
