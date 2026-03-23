package com.weeth.domain.club.application.usecase.command

import com.weeth.domain.attendance.domain.repository.AttendanceRepository
import com.weeth.domain.cardinal.application.exception.CardinalNotFoundException
import com.weeth.domain.cardinal.domain.repository.CardinalReader
import com.weeth.domain.cardinal.fixture.CardinalTestFixture
import com.weeth.domain.club.application.dto.request.ClubMemberApplyObRequest
import com.weeth.domain.club.application.dto.request.ClubMemberRoleUpdateRequest
import com.weeth.domain.club.application.exception.ClubMemberNotInClubException
import com.weeth.domain.club.application.exception.LeadSelfTransferException
import com.weeth.domain.club.application.exception.LeadTransferOnlyException
import com.weeth.domain.club.application.exception.MemberNotActiveException
import com.weeth.domain.club.application.exception.NotLeadException
import com.weeth.domain.club.domain.enums.MemberRole
import com.weeth.domain.club.domain.enums.MemberStatus
import com.weeth.domain.club.domain.repository.ClubMemberCardinalRepository
import com.weeth.domain.club.domain.service.ClubMemberCardinalPolicy
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.club.fixture.ClubMemberTestFixture
import com.weeth.domain.club.fixture.ClubTestFixture
import com.weeth.domain.session.domain.repository.SessionReader
import com.weeth.domain.session.fixture.SessionTestFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.test.util.ReflectionTestUtils

class AdminClubMemberUseCaseTest :
    DescribeSpec({
        val clubMemberPolicy = mockk<ClubMemberPolicy>()
        val clubMemberCardinalPolicy = mockk<ClubMemberCardinalPolicy>(relaxed = true)
        val cardinalReader = mockk<CardinalReader>(relaxed = true)
        val sessionReader = mockk<SessionReader>(relaxed = true)
        val attendanceRepository = mockk<AttendanceRepository>(relaxed = true)
        val clubMemberCardinalRepository = mockk<ClubMemberCardinalRepository>(relaxed = true)
        val useCase =
            AdminClubMemberUseCase(
                clubMemberPolicy,
                clubMemberCardinalPolicy,
                cardinalReader,
                sessionReader,
                attendanceRepository,
                clubMemberCardinalRepository,
            )
        val adminMember = ClubMemberTestFixture.createAdminMember()

        beforeTest {
            clearMocks(
                clubMemberPolicy,
                clubMemberCardinalPolicy,
                cardinalReader,
                sessionReader,
                attendanceRepository,
                clubMemberCardinalRepository,
            )
            every {
                attendanceRepository.saveAll(
                    any<List<com.weeth.domain.attendance.domain.entity.Attendance>>(),
                )
            } answers
                { firstArg() }
            every { clubMemberCardinalRepository.save(any()) } answers { firstArg() }
        }

        describe("accept") {
            it("같은 동아리 소속 멤버를 승인한다") {
                val member = ClubMemberTestFixture.createWaitingMember()
                every { clubMemberPolicy.requireAdmin(1L, 10L) } returns adminMember
                every { clubMemberPolicy.getMemberInClub(1L, 20L) } returns member

                useCase.accept(1L, 10L, 20L)

                member.memberStatus shouldBe MemberStatus.ACTIVE
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

            it("LEAD로 직접 변경 시도하면 예외가 발생한다") {
                val member = ClubMemberTestFixture.createActiveMember(memberRole = MemberRole.USER)
                every { clubMemberPolicy.requireAdmin(1L, 10L) } returns adminMember
                every { clubMemberPolicy.getMemberInClub(1L, 20L) } returns member

                shouldThrow<LeadTransferOnlyException> {
                    useCase.updateMemberRole(
                        1L,
                        10L,
                        ClubMemberRoleUpdateRequest(clubMemberId = 20L, memberRole = MemberRole.LEAD),
                    )
                }
            }

            it("LEAD 멤버의 역할을 직접 변경 시도하면 예외가 발생한다") {
                val leadMember = ClubMemberTestFixture.createLeadMember()
                every { clubMemberPolicy.requireAdmin(1L, 10L) } returns adminMember
                every { clubMemberPolicy.getMemberInClub(1L, 20L) } returns leadMember

                shouldThrow<LeadTransferOnlyException> {
                    useCase.updateMemberRole(
                        1L,
                        10L,
                        ClubMemberRoleUpdateRequest(clubMemberId = 20L, memberRole = MemberRole.ADMIN),
                    )
                }
            }
        }

        describe("transferLead") {
            val club = ClubTestFixture.createClub()

            it("LEAD가 다른 멤버에게 권한을 이양한다") {
                val lead = ClubMemberTestFixture.createLeadMember(club = club)
                val target = ClubMemberTestFixture.createActiveMember(club = club)
                ReflectionTestUtils.setField(lead, "id", 10L)
                ReflectionTestUtils.setField(target, "id", 20L)
                every { clubMemberPolicy.getActiveMemberWithLock(1L, 10L) } returns lead
                every { clubMemberPolicy.getActiveMemberInClubWithLock(1L, 20L) } returns target

                useCase.transferLead(1L, 10L, 20L)

                lead.memberRole shouldBe MemberRole.ADMIN
                target.memberRole shouldBe MemberRole.LEAD
            }

            it("LEAD가 아닌 멤버가 이양을 시도하면 예외가 발생한다") {
                val nonLead = ClubMemberTestFixture.createActiveMember(club = club)
                every { clubMemberPolicy.getActiveMemberWithLock(1L, 10L) } returns nonLead

                shouldThrow<NotLeadException> {
                    useCase.transferLead(1L, 10L, 20L)
                }
            }

            it("자기 자신에게 이양을 시도하면 예외가 발생한다") {
                val lead = ClubMemberTestFixture.createLeadMember(club = club)
                ReflectionTestUtils.setField(lead, "id", 10L)
                every { clubMemberPolicy.getActiveMemberWithLock(1L, 10L) } returns lead
                every { clubMemberPolicy.getActiveMemberInClubWithLock(1L, 10L) } returns lead

                shouldThrow<LeadSelfTransferException> {
                    useCase.transferLead(1L, 10L, 10L)
                }
            }

            it("비활성 멤버에게 이양을 시도하면 예외가 발생한다") {
                val lead = ClubMemberTestFixture.createLeadMember(club = club)
                every { clubMemberPolicy.getActiveMemberWithLock(1L, 10L) } returns lead
                every {
                    clubMemberPolicy.getActiveMemberInClubWithLock(1L, 20L)
                } throws MemberNotActiveException()

                shouldThrow<MemberNotActiveException> {
                    useCase.transferLead(1L, 10L, 20L)
                }
            }

            it("존재하지 않는 멤버에게 이양을 시도하면 예외가 발생한다") {
                val lead = ClubMemberTestFixture.createLeadMember(club = club)
                every { clubMemberPolicy.getActiveMemberWithLock(1L, 10L) } returns lead
                every {
                    clubMemberPolicy.getActiveMemberInClubWithLock(1L, 99L)
                } throws ClubMemberNotInClubException()

                shouldThrow<ClubMemberNotInClubException> {
                    useCase.transferLead(1L, 10L, 99L)
                }
            }
        }

        describe("applyOb") {
            it("새 기수를 정상 등록한다") {
                val member = ClubMemberTestFixture.createActiveMember(club = adminMember.club)
                val cardinal =
                    CardinalTestFixture.createCardinal(
                        id = 1L,
                        club = adminMember.club,
                        cardinalNumber = 8,
                        year = 2026,
                        semester = 1,
                    )
                val session = SessionTestFixture.createSession(club = adminMember.club, cardinal = 8)
                every { clubMemberPolicy.requireAdmin(1L, 10L) } returns adminMember
                every { clubMemberPolicy.getMemberInClub(1L, 20L) } returns member
                every { cardinalReader.findByClubIdAndCardinalNumber(1L, 8) } returns cardinal
                every { clubMemberCardinalPolicy.notContains(member, cardinal) } returns true
                every { clubMemberCardinalPolicy.isLatestOrFirstCardinal(member, cardinal) } returns true
                every { sessionReader.findAllByClubIdAndCardinalIn(1L, listOf(8)) } returns listOf(session)

                useCase.applyOb(1L, 10L, listOf(ClubMemberApplyObRequest(20L, 8)))

                verify(exactly = 1) { clubMemberCardinalRepository.save(any()) }
                verify(
                    exactly = 1,
                ) { attendanceRepository.saveAll(any<List<com.weeth.domain.attendance.domain.entity.Attendance>>()) }
            }

            it("이미 등록된 기수는 무시한다") {
                val member = ClubMemberTestFixture.createActiveMember(club = adminMember.club)
                val cardinal =
                    CardinalTestFixture.createCardinal(
                        id = 1L,
                        club = adminMember.club,
                        cardinalNumber = 8,
                        year = 2026,
                        semester = 1,
                    )
                every { clubMemberPolicy.requireAdmin(1L, 10L) } returns adminMember
                every { clubMemberPolicy.getMemberInClub(1L, 20L) } returns member
                every { cardinalReader.findByClubIdAndCardinalNumber(1L, 8) } returns cardinal
                every { clubMemberCardinalPolicy.notContains(member, cardinal) } returns false

                useCase.applyOb(1L, 10L, listOf(ClubMemberApplyObRequest(20L, 8)))

                verify(exactly = 0) { clubMemberCardinalRepository.save(any()) }
                verify(
                    exactly = 0,
                ) { attendanceRepository.saveAll(any<List<com.weeth.domain.attendance.domain.entity.Attendance>>()) }
            }

            it("동일한 요청이 중복으로 전달되면 1회만 처리한다") {
                val session = SessionTestFixture.createSession(club = adminMember.club, cardinal = 8)
                val member = ClubMemberTestFixture.createActiveMember(club = adminMember.club)
                val cardinal =
                    CardinalTestFixture.createCardinal(
                        id = 1L,
                        club = adminMember.club,
                        cardinalNumber = 8,
                        year = 2026,
                        semester = 1,
                    )
                every { clubMemberPolicy.requireAdmin(1L, 10L) } returns adminMember
                every { clubMemberPolicy.getMemberInClub(1L, 20L) } returns member
                every { cardinalReader.findByClubIdAndCardinalNumber(1L, 8) } returns cardinal
                every { clubMemberCardinalPolicy.notContains(member, cardinal) } returns true
                every { clubMemberCardinalPolicy.isLatestOrFirstCardinal(member, cardinal) } returns true
                every { sessionReader.findAllByClubIdAndCardinalIn(1L, listOf(8)) } returns listOf(session)

                useCase.applyOb(1L, 10L, listOf(ClubMemberApplyObRequest(20L, 8), ClubMemberApplyObRequest(20L, 8)))

                verify(exactly = 1) { clubMemberCardinalRepository.save(any()) }
                verify(
                    exactly = 1,
                ) { attendanceRepository.saveAll(any<List<com.weeth.domain.attendance.domain.entity.Attendance>>()) }
            }

            it("존재하지 않는 기수면 예외가 발생한다") {
                val member = ClubMemberTestFixture.createActiveMember(club = adminMember.club)
                every { clubMemberPolicy.requireAdmin(1L, 10L) } returns adminMember
                every { clubMemberPolicy.getMemberInClub(1L, 20L) } returns member
                every { cardinalReader.findByClubIdAndCardinalNumber(1L, 8) } returns null

                shouldThrow<CardinalNotFoundException> {
                    useCase.applyOb(1L, 10L, listOf(ClubMemberApplyObRequest(20L, 8)))
                }
            }

            it("현재 기수 등록 시 출석 통계를 초기화한다") {
                val member = ClubMemberTestFixture.createActiveMember(club = adminMember.club)
                val cardinal =
                    CardinalTestFixture.createCardinal(
                        id = 1L,
                        club = adminMember.club,
                        cardinalNumber = 8,
                        year = 2026,
                        semester = 1,
                    )
                repeat(2) { member.attend() }
                repeat(1) { member.absent() }
                every { clubMemberPolicy.requireAdmin(1L, 10L) } returns adminMember
                every { clubMemberPolicy.getMemberInClub(1L, 20L) } returns member
                every { cardinalReader.findByClubIdAndCardinalNumber(1L, 8) } returns cardinal
                every { clubMemberCardinalPolicy.notContains(member, cardinal) } returns true
                every { clubMemberCardinalPolicy.isLatestOrFirstCardinal(member, cardinal) } returns true
                every { sessionReader.findAllByClubIdAndCardinalIn(1L, listOf(8)) } returns emptyList()

                useCase.applyOb(1L, 10L, listOf(ClubMemberApplyObRequest(20L, 8)))

                member.attendanceStats.attendanceCount shouldBe 0
                member.attendanceStats.absenceCount shouldBe 0
                member.attendanceStats.attendanceRate shouldBe 0
            }
        }
    })
