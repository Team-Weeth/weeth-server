package com.weeth.domain.club.domain.service

import com.weeth.domain.club.application.exception.ClubMemberNotFoundException
import com.weeth.domain.club.application.exception.ClubMemberNotInClubException
import com.weeth.domain.club.application.exception.MemberNotActiveException
import com.weeth.domain.club.application.exception.NotClubAdminException
import com.weeth.domain.club.domain.repository.ClubMemberReader
import com.weeth.domain.club.fixture.ClubTestFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk

class ClubMemberPolicyTest :
    DescribeSpec({
        val clubMemberReader = mockk<ClubMemberReader>()
        val policy = ClubMemberPolicy(clubMemberReader)

        beforeTest {
            clearMocks(clubMemberReader)
        }

        describe("getActiveMember") {
            context("활성 멤버가 존재하는 경우") {
                it("활성 멤버를 반환해야 한다") {
                    val activeMember =
                        ClubTestFixture.createClubMember(
                            memberStatus = com.weeth.domain.club.domain.enums.MemberStatus.ACTIVE,
                        )
                    every { clubMemberReader.findByClubIdAndUserId(1L, 1L) } returns activeMember

                    val result = policy.getActiveMember(1L, 1L)
                    assert(result.id == activeMember.id)
                }
            }

            context("멤버가 존재하지 않는 경우") {
                it("ClubMemberNotFoundException을 발생시켜야 한다") {
                    every { clubMemberReader.findByClubIdAndUserId(1L, 1L) } returns null

                    shouldThrow<ClubMemberNotFoundException> {
                        policy.getActiveMember(1L, 1L)
                    }
                }
            }

            context("멤버는 존재하지만 비활성 상태인 경우") {
                it("MemberNotActiveException을 발생시켜야 한다") {
                    val inactiveMember =
                        ClubTestFixture.createClubMember(
                            memberStatus = com.weeth.domain.club.domain.enums.MemberStatus.WAITING,
                        )
                    every { clubMemberReader.findByClubIdAndUserId(1L, 1L) } returns inactiveMember

                    shouldThrow<MemberNotActiveException> {
                        policy.getActiveMember(1L, 1L)
                    }
                }
            }
        }

        describe("requireAdmin") {
            context("활성 상태의 관리자인 경우") {
                it("멤버를 반환해야 한다") {
                    val adminMember =
                        ClubTestFixture.createClubMember(
                            memberStatus = com.weeth.domain.club.domain.enums.MemberStatus.ACTIVE,
                            memberRole = com.weeth.domain.club.domain.enums.MemberRole.ADMIN,
                        )
                    every { clubMemberReader.findByClubIdAndUserId(1L, 1L) } returns adminMember

                    val result = policy.requireAdmin(1L, 1L)
                    assert(result.id == adminMember.id)
                }
            }

            context("활성 상태이지만 관리자가 아닌 경우") {
                it("NotClubAdminException을 발생시켜야 한다") {
                    val userMember =
                        ClubTestFixture.createClubMember(
                            memberStatus = com.weeth.domain.club.domain.enums.MemberStatus.ACTIVE,
                            memberRole = com.weeth.domain.club.domain.enums.MemberRole.USER,
                        )
                    every { clubMemberReader.findByClubIdAndUserId(1L, 1L) } returns userMember

                    shouldThrow<NotClubAdminException> {
                        policy.requireAdmin(1L, 1L)
                    }
                }
            }

            context("비활성 상태인 경우") {
                it("MemberNotActiveException을 발생시켜야 한다") {
                    val inactiveMember =
                        ClubTestFixture.createClubMember(
                            memberStatus = com.weeth.domain.club.domain.enums.MemberStatus.WAITING,
                            memberRole = com.weeth.domain.club.domain.enums.MemberRole.ADMIN,
                        )
                    every { clubMemberReader.findByClubIdAndUserId(1L, 1L) } returns inactiveMember

                    shouldThrow<MemberNotActiveException> {
                        policy.requireAdmin(1L, 1L)
                    }
                }
            }
        }

        describe("getMemberInClub") {
            context("해당 동아리에 속한 멤버인 경우") {
                it("멤버를 반환해야 한다") {
                    val member = ClubTestFixture.createClubMember()
                    every { clubMemberReader.findByIdOrNull(1L) } returns member

                    val result = policy.getMemberInClub(member.club.id, 1L)

                    assert(result == member)
                }
            }

            context("멤버는 존재하지만 다른 동아리에 속한 경우") {
                it("ClubMemberNotInClubException을 발생시켜야 한다") {
                    val member = ClubTestFixture.createClubMember()
                    every { clubMemberReader.findByIdOrNull(1L) } returns member

                    shouldThrow<ClubMemberNotInClubException> {
                        // member.club.id와 다른 clubId를 전달하여 다른 동아리 시나리오를 재현
                        policy.getMemberInClub(member.club.id + 999L, 1L)
                    }
                }
            }

            context("멤버 자체가 존재하지 않는 경우") {
                it("ClubMemberNotFoundException을 발생시켜야 한다") {
                    every { clubMemberReader.findByIdOrNull(2L) } returns null

                    shouldThrow<ClubMemberNotFoundException> {
                        policy.getMemberInClub(1L, 2L)
                    }
                }
            }
        }
    })
