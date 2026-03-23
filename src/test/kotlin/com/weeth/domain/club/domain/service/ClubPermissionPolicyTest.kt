package com.weeth.domain.club.domain.service

import com.weeth.domain.club.application.exception.MemberNotActiveException
import com.weeth.domain.club.application.exception.NotClubAdminException
import com.weeth.domain.club.domain.enums.MemberRole
import com.weeth.domain.club.domain.enums.MemberStatus
import com.weeth.domain.club.fixture.ClubTestFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk

class ClubPermissionPolicyTest :
    DescribeSpec({
        val clubMemberPolicy = mockk<ClubMemberPolicy>()
        val policy = ClubPermissionPolicy(clubMemberPolicy)

        beforeTest {
            clearMocks(clubMemberPolicy)
        }

        describe("requireAdmin") {
            context("활성 상태의 관리자인 경우") {
                it("멤버를 반환해야 한다") {
                    val adminMember =
                        ClubTestFixture.createClubMember(
                            memberStatus = MemberStatus.ACTIVE,
                            memberRole = MemberRole.ADMIN,
                        )
                    every { clubMemberPolicy.getActiveMember(1L, 1L) } returns adminMember

                    val result = policy.requireAdmin(1L, 1L)
                    result.id shouldBe adminMember.id
                }
            }

            context("활성 상태의 LEAD인 경우") {
                it("멤버를 반환해야 한다") {
                    val leadMember =
                        ClubTestFixture.createClubMember(
                            memberStatus = MemberStatus.ACTIVE,
                            memberRole = MemberRole.LEAD,
                        )
                    every { clubMemberPolicy.getActiveMember(1L, 1L) } returns leadMember

                    val result = policy.requireAdmin(1L, 1L)
                    result.id shouldBe leadMember.id
                }
            }

            context("활성 상태이지만 관리자가 아닌 경우") {
                it("NotClubAdminException을 발생시켜야 한다") {
                    val userMember =
                        ClubTestFixture.createClubMember(
                            memberStatus = MemberStatus.ACTIVE,
                            memberRole = MemberRole.USER,
                        )
                    every { clubMemberPolicy.getActiveMember(1L, 1L) } returns userMember

                    shouldThrow<NotClubAdminException> {
                        policy.requireAdmin(1L, 1L)
                    }
                }
            }

            context("비활성 상태인 경우") {
                it("MemberNotActiveException을 발생시켜야 한다") {
                    every {
                        clubMemberPolicy.getActiveMember(1L, 1L)
                    } throws MemberNotActiveException()

                    shouldThrow<MemberNotActiveException> {
                        policy.requireAdmin(1L, 1L)
                    }
                }
            }
        }
    })
