package com.weeth.domain.club.domain.service

import com.weeth.domain.club.application.exception.ClubMemberNotFoundException
import com.weeth.domain.club.application.exception.ClubMemberNotInClubException
import com.weeth.domain.club.application.exception.MemberNotActiveException
import com.weeth.domain.club.domain.enums.MemberStatus
import com.weeth.domain.club.domain.repository.ClubMemberReader
import com.weeth.domain.club.fixture.ClubTestFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
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
                            memberStatus = MemberStatus.ACTIVE,
                        )
                    every { clubMemberReader.findByClubIdAndUserId(1L, 1L) } returns activeMember

                    val result = policy.getActiveMember(1L, 1L)
                    result.id shouldBe activeMember.id
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
                            memberStatus = MemberStatus.WAITING,
                        )
                    every { clubMemberReader.findByClubIdAndUserId(1L, 1L) } returns inactiveMember

                    shouldThrow<MemberNotActiveException> {
                        policy.getActiveMember(1L, 1L)
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

                    result shouldBe member
                }
            }

            context("멤버는 존재하지만 다른 동아리에 속한 경우") {
                it("ClubMemberNotInClubException을 발생시켜야 한다") {
                    val member = ClubTestFixture.createClubMember()
                    every { clubMemberReader.findByIdOrNull(1L) } returns member

                    shouldThrow<ClubMemberNotInClubException> {
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
