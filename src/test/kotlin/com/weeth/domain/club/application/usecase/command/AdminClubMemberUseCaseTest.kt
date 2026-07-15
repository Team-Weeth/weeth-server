package com.weeth.domain.club.application.usecase.command

import com.weeth.domain.attendance.domain.repository.AttendanceRepository
import com.weeth.domain.attendance.domain.service.AttendanceInitializer
import com.weeth.domain.attendance.fixture.AttendanceTestFixture
import com.weeth.domain.cardinal.application.exception.CardinalNotFoundException
import com.weeth.domain.cardinal.domain.repository.CardinalReader
import com.weeth.domain.cardinal.fixture.CardinalTestFixture
import com.weeth.domain.club.application.dto.request.ClubMemberApplyObRequest
import com.weeth.domain.club.application.dto.request.ClubMemberRoleUpdateRequest
import com.weeth.domain.club.application.dto.request.UpdateMemberCardinalRequest
import com.weeth.domain.club.application.exception.CannotBanLeadException
import com.weeth.domain.club.application.exception.CardinalRemovalHasAttendanceException
import com.weeth.domain.club.application.exception.ClubMemberNotInClubException
import com.weeth.domain.club.application.exception.LeadSelfTransferException
import com.weeth.domain.club.application.exception.LeadTransferOnlyException
import com.weeth.domain.club.application.exception.MemberNotActiveException
import com.weeth.domain.club.application.exception.NotLeadException
import com.weeth.domain.club.application.exception.SelfBanNotAllowedException
import com.weeth.domain.club.application.exception.SelfRoleChangeNotAllowedException
import com.weeth.domain.club.domain.entity.ClubMemberCardinal
import com.weeth.domain.club.domain.enums.MemberRole
import com.weeth.domain.club.domain.enums.MemberStatus
import com.weeth.domain.club.domain.repository.ClubMemberCardinalRepository
import com.weeth.domain.club.domain.repository.ClubMemberReader
import com.weeth.domain.club.domain.service.ClubMemberCardinalPolicy
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.club.domain.service.ClubPermissionPolicy
import com.weeth.domain.club.fixture.ClubMemberTestFixture
import com.weeth.domain.club.fixture.ClubTestFixture
import com.weeth.domain.penalty.domain.repository.PenaltyReader
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
        val clubPermissionPolicy = mockk<ClubPermissionPolicy>()
        val clubMemberCardinalPolicy = mockk<ClubMemberCardinalPolicy>(relaxed = true)
        val cardinalReader = mockk<CardinalReader>(relaxed = true)
        val clubMemberReader = mockk<ClubMemberReader>(relaxed = true)
        val sessionReader = mockk<SessionReader>(relaxed = true)
        val attendanceRepository = mockk<AttendanceRepository>(relaxed = true)
        val attendanceInitializer = mockk<AttendanceInitializer>(relaxed = true)
        val penaltyReader = mockk<PenaltyReader>(relaxed = true)
        val clubMemberCardinalRepository = mockk<ClubMemberCardinalRepository>(relaxed = true)
        val useCase =
            AdminClubMemberUseCase(
                clubMemberPolicy,
                clubPermissionPolicy,
                clubMemberCardinalPolicy,
                cardinalReader,
                clubMemberReader,
                sessionReader,
                attendanceRepository,
                attendanceInitializer,
                penaltyReader,
                clubMemberCardinalRepository,
            )
        val club = ClubTestFixture.createClub(id = 1L)
        val adminMember = ClubMemberTestFixture.createAdminMember(club = club)

        beforeTest {
            clearMocks(
                clubMemberPolicy,
                clubPermissionPolicy,
                clubMemberCardinalPolicy,
                cardinalReader,
                clubMemberReader,
                sessionReader,
                attendanceRepository,
                attendanceInitializer,
                penaltyReader,
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
                every { clubPermissionPolicy.requireAdmin(1L, 10L) } returns adminMember
                every { clubMemberPolicy.getMemberInClub(1L, 20L) } returns member

                useCase.accept(1L, 10L, 20L)

                member.memberStatus shouldBe MemberStatus.ACTIVE
            }

            it("다른 동아리 소속 멤버면 예외가 발생한다") {
                every { clubPermissionPolicy.requireAdmin(1L, 10L) } returns adminMember
                every { clubMemberPolicy.getMemberInClub(1L, 20L) } throws ClubMemberNotInClubException()

                shouldThrow<ClubMemberNotInClubException> {
                    useCase.accept(1L, 10L, 20L)
                }
            }
        }

        describe("ban") {
            it("같은 동아리 소속 멤버를 추방한다") {
                ReflectionTestUtils.setField(adminMember, "id", 10L)
                val member = ClubMemberTestFixture.createActiveMember(id = 20L)
                every { clubPermissionPolicy.requireAdmin(1L, 10L) } returns adminMember
                every { clubMemberPolicy.getActiveMemberInClubWithLock(1L, 20L) } returns member

                useCase.ban(1L, 10L, 20L)

                member.memberStatus shouldBe MemberStatus.BANNED
            }

            it("자기 자신은 추방할 수 없다") {
                ReflectionTestUtils.setField(adminMember, "id", 10L)
                every { clubPermissionPolicy.requireAdmin(1L, 10L) } returns adminMember
                every { clubMemberPolicy.getActiveMemberInClubWithLock(1L, 10L) } returns adminMember

                shouldThrow<SelfBanNotAllowedException> {
                    useCase.ban(1L, 10L, 10L)
                }
            }

            it("리더는 권한 이양 전 추방할 수 없다") {
                ReflectionTestUtils.setField(adminMember, "id", 10L)
                val leadMember = ClubMemberTestFixture.createLeadMember(club = club)
                every { clubPermissionPolicy.requireAdmin(1L, 10L) } returns adminMember
                every { clubMemberPolicy.getActiveMemberInClubWithLock(1L, 20L) } returns leadMember

                shouldThrow<CannotBanLeadException> {
                    useCase.ban(1L, 10L, 20L)
                }
            }
        }

        describe("restore") {
            it("추방된 멤버를 복구한다") {
                val member = ClubMemberTestFixture.createBannedMember(club = club)
                every { clubPermissionPolicy.requireAdmin(1L, 10L) } returns adminMember
                every { clubMemberPolicy.getMemberInClub(1L, 20L) } returns member

                useCase.restore(1L, 10L, 20L)

                member.memberStatus shouldBe MemberStatus.ACTIVE
            }
        }

        describe("updateMemberRole") {
            it("같은 동아리 소속 멤버의 권한을 변경한다") {
                val member = ClubMemberTestFixture.createActiveMember(memberRole = MemberRole.USER)
                every { clubPermissionPolicy.requireAdmin(1L, 10L) } returns adminMember
                every { clubMemberPolicy.getMemberInClub(1L, 20L) } returns member

                useCase.updateMemberRole(
                    1L,
                    10L,
                    20L,
                    ClubMemberRoleUpdateRequest(memberRole = MemberRole.ADMIN),
                )

                member.memberRole shouldBe MemberRole.ADMIN
            }

            it("LEAD로 직접 변경 시도하면 예외가 발생한다") {
                val member = ClubMemberTestFixture.createActiveMember(memberRole = MemberRole.USER)
                every { clubPermissionPolicy.requireAdmin(1L, 10L) } returns adminMember
                every { clubMemberPolicy.getMemberInClub(1L, 20L) } returns member

                shouldThrow<LeadTransferOnlyException> {
                    useCase.updateMemberRole(
                        1L,
                        10L,
                        20L,
                        ClubMemberRoleUpdateRequest(memberRole = MemberRole.LEAD),
                    )
                }
            }

            it("LEAD 멤버의 역할을 직접 변경 시도하면 예외가 발생한다") {
                val leadMember = ClubMemberTestFixture.createLeadMember()
                every { clubPermissionPolicy.requireAdmin(1L, 10L) } returns adminMember
                every { clubMemberPolicy.getMemberInClub(1L, 20L) } returns leadMember

                shouldThrow<LeadTransferOnlyException> {
                    useCase.updateMemberRole(
                        1L,
                        10L,
                        20L,
                        ClubMemberRoleUpdateRequest(memberRole = MemberRole.ADMIN),
                    )
                }
            }

            it("자기 자신의 권한은 변경할 수 없다") {
                ReflectionTestUtils.setField(adminMember, "id", 10L)
                every { clubPermissionPolicy.requireAdmin(1L, 10L) } returns adminMember
                every { clubMemberPolicy.getMemberInClub(1L, 10L) } returns adminMember

                shouldThrow<SelfRoleChangeNotAllowedException> {
                    useCase.updateMemberRole(
                        1L,
                        10L,
                        10L,
                        ClubMemberRoleUpdateRequest(memberRole = MemberRole.USER),
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
                val member = ClubMemberTestFixture.createActiveMember(id = 20L, club = adminMember.club)
                val cardinal =
                    CardinalTestFixture.createCardinal(
                        id = 1L,
                        club = adminMember.club,
                        cardinalNumber = 8,
                    )
                every { clubPermissionPolicy.requireAdmin(1L, 10L) } returns adminMember
                every { clubMemberReader.findAllByIdsWithLock(listOf(20L)) } returns listOf(member)
                every { cardinalReader.findByClubIdAndCardinalNumber(1L, 8) } returns cardinal
                every { clubMemberCardinalPolicy.notContains(member, cardinal) } returns true
                every { clubMemberCardinalPolicy.isLatestOrFirstCardinal(member, cardinal) } returns true

                useCase.applyOb(1L, 10L, listOf(ClubMemberApplyObRequest(20L, 8)))

                verify(exactly = 1) { clubMemberCardinalRepository.save(any()) }
                verify(exactly = 1) {
                    attendanceInitializer.initializeForMemberCardinals(1L, member, listOf(cardinal))
                }
            }

            it("이미 등록된 기수는 무시한다") {
                val member = ClubMemberTestFixture.createActiveMember(id = 20L, club = adminMember.club)
                val cardinal =
                    CardinalTestFixture.createCardinal(
                        id = 1L,
                        club = adminMember.club,
                        cardinalNumber = 8,
                    )
                every { clubPermissionPolicy.requireAdmin(1L, 10L) } returns adminMember
                every { clubMemberReader.findAllByIdsWithLock(listOf(20L)) } returns listOf(member)
                every { cardinalReader.findByClubIdAndCardinalNumber(1L, 8) } returns cardinal
                every { clubMemberCardinalPolicy.notContains(member, cardinal) } returns false

                useCase.applyOb(1L, 10L, listOf(ClubMemberApplyObRequest(20L, 8)))

                verify(exactly = 0) { clubMemberCardinalRepository.save(any()) }
                verify(exactly = 0) {
                    attendanceInitializer.initializeForMemberCardinals(any(), any(), any())
                }
            }

            it("동일한 요청이 중복으로 전달되면 1회만 처리한다") {
                val member = ClubMemberTestFixture.createActiveMember(id = 20L, club = adminMember.club)
                val cardinal =
                    CardinalTestFixture.createCardinal(
                        id = 1L,
                        club = adminMember.club,
                        cardinalNumber = 8,
                    )
                every { clubPermissionPolicy.requireAdmin(1L, 10L) } returns adminMember
                every { clubMemberReader.findAllByIdsWithLock(listOf(20L)) } returns listOf(member)
                every { cardinalReader.findByClubIdAndCardinalNumber(1L, 8) } returns cardinal
                every { clubMemberCardinalPolicy.notContains(member, cardinal) } returns true
                every { clubMemberCardinalPolicy.isLatestOrFirstCardinal(member, cardinal) } returns true

                useCase.applyOb(1L, 10L, listOf(ClubMemberApplyObRequest(20L, 8), ClubMemberApplyObRequest(20L, 8)))

                verify(exactly = 1) { clubMemberCardinalRepository.save(any()) }
                verify(exactly = 1) {
                    attendanceInitializer.initializeForMemberCardinals(1L, member, listOf(cardinal))
                }
            }

            it("존재하지 않는 기수면 예외가 발생한다") {
                val member = ClubMemberTestFixture.createActiveMember(id = 20L, club = adminMember.club)
                every { clubPermissionPolicy.requireAdmin(1L, 10L) } returns adminMember
                every { clubMemberReader.findAllByIdsWithLock(listOf(20L)) } returns listOf(member)
                every { cardinalReader.findByClubIdAndCardinalNumber(1L, 8) } returns null

                shouldThrow<CardinalNotFoundException> {
                    useCase.applyOb(1L, 10L, listOf(ClubMemberApplyObRequest(20L, 8)))
                }
            }

            it("다른 클럽 소속 멤버 ID가 포함된 경우 예외가 발생한다") {
                val otherClubMember = ClubMemberTestFixture.createActiveMember(id = 20L)
                every { clubPermissionPolicy.requireAdmin(1L, 10L) } returns adminMember
                every { clubMemberReader.findAllByIdsWithLock(listOf(20L)) } returns listOf(otherClubMember)

                shouldThrow<ClubMemberNotInClubException> {
                    useCase.applyOb(1L, 10L, listOf(ClubMemberApplyObRequest(20L, 8)))
                }
            }

            it("최신/첫 기수 등록 시 출석 통계를 초기화한다") {
                val member = ClubMemberTestFixture.createActiveMember(id = 20L, club = adminMember.club)
                val cardinal =
                    CardinalTestFixture.createCardinal(
                        id = 1L,
                        club = adminMember.club,
                        cardinalNumber = 8,
                    )
                repeat(2) { member.attend() }
                repeat(1) { member.absent() }
                every { clubPermissionPolicy.requireAdmin(1L, 10L) } returns adminMember
                every { clubMemberReader.findAllByIdsWithLock(listOf(20L)) } returns listOf(member)
                every { cardinalReader.findByClubIdAndCardinalNumber(1L, 8) } returns cardinal
                every { clubMemberCardinalPolicy.notContains(member, cardinal) } returns true
                every { clubMemberCardinalPolicy.isLatestOrFirstCardinal(member, cardinal) } returns true

                useCase.applyOb(1L, 10L, listOf(ClubMemberApplyObRequest(20L, 8)))

                member.attendanceStats.attendanceCount shouldBe 0
                member.attendanceStats.absenceCount shouldBe 0
                member.attendanceStats.attendanceRate shouldBe 0
            }
        }

        describe("updateCardinals") {
            // 각 it에서 member를 독립 생성하여 상태 오염 방지
            fun createMember() = ClubMemberTestFixture.createActiveMember(id = 20L, club = club)

            fun stubMemberLock(member: com.weeth.domain.club.domain.entity.ClubMember) {
                every { clubPermissionPolicy.requireAdmin(1L, 10L) } returns adminMember
                every { clubMemberReader.findByIdWithLock(20L) } returns member
            }

            it("기수를 추가하면 해당 기수의 세션에 출석이 초기화된다") {
                val member = createMember()
                val cardinal = CardinalTestFixture.createCardinal(id = 1L, club = club, cardinalNumber = 8)
                stubMemberLock(member)
                every { cardinalReader.findAllByClubIdAndIdIn(1L, listOf(1L)) } returns listOf(cardinal)
                every { clubMemberCardinalRepository.findAllByClubMembers(listOf(member)) } returns emptyList()
                every { clubMemberCardinalPolicy.isLatestOrFirstCardinal(member, cardinal) } returns false

                useCase.updateCardinals(1L, 10L, 20L, UpdateMemberCardinalRequest(cardinalIds = listOf(1L)))

                verify(exactly = 1) {
                    attendanceInitializer.initializeForMemberCardinals(1L, member, listOf(cardinal))
                }
            }

            it("추가할 기수가 없으면 출석 초기화를 요청하지 않는다") {
                val member = createMember()
                val cardinal = CardinalTestFixture.createCardinal(id = 1L, club = club, cardinalNumber = 8)
                val existingLink = ClubMemberCardinal.create(member, cardinal)
                stubMemberLock(member)
                every { cardinalReader.findAllByClubIdAndIdIn(1L, listOf(1L)) } returns listOf(cardinal)
                every { clubMemberCardinalRepository.findAllByClubMembers(listOf(member)) } returns listOf(existingLink)

                useCase.updateCardinals(1L, 10L, 20L, UpdateMemberCardinalRequest(cardinalIds = listOf(1L)))

                verify(exactly = 0) {
                    attendanceInitializer.initializeForMemberCardinals(any(), any(), any())
                }
            }

            it("최신 기수를 새로 추가하면 출석 통계와 패널티가 리셋된다") {
                val member =
                    createMember().also {
                        repeat(3) { _ -> it.attend() }
                        it.incrementPenaltyCount()
                    }
                val existingCardinal = CardinalTestFixture.createCardinal(id = 1L, club = club, cardinalNumber = 8)
                val newCardinal = CardinalTestFixture.createCardinal(id = 2L, club = club, cardinalNumber = 9)
                val existingLink = ClubMemberCardinal.create(member, existingCardinal)
                stubMemberLock(member)
                every { cardinalReader.findAllByClubIdAndIdIn(1L, listOf(1L, 2L)) } returns
                    listOf(existingCardinal, newCardinal)
                every { clubMemberCardinalRepository.findAllByClubMembers(listOf(member)) } returns listOf(existingLink)
                every { clubMemberCardinalPolicy.isLatestOrFirstCardinal(member, newCardinal) } returns true

                useCase.updateCardinals(1L, 10L, 20L, UpdateMemberCardinalRequest(cardinalIds = listOf(1L, 2L)))

                member.attendanceStats.attendanceCount shouldBe 0
                member.attendanceStats.absenceCount shouldBe 0
                member.penaltyCount shouldBe 0
                verify(exactly = 1) {
                    attendanceInitializer.initializeForMemberCardinals(1L, member, listOf(newCardinal))
                }
            }

            it("출석 기록 없는 기수 삭제 시 force 없이도 바로 삭제된다") {
                val member = createMember()
                // 현재: 8기, 9기 보유 → 요청: 9기만 유지 → 8기 삭제
                val keepCardinal = CardinalTestFixture.createCardinal(id = 2L, club = club, cardinalNumber = 9)
                val removeCardinal = CardinalTestFixture.createCardinal(id = 1L, club = club, cardinalNumber = 8)
                val keepLink = ClubMemberCardinal.create(member, keepCardinal)
                val removeLink = ClubMemberCardinal.create(member, removeCardinal)
                val session = SessionTestFixture.createSession(club = club, cardinal = 8)
                stubMemberLock(member)
                every { cardinalReader.findAllByClubIdAndIdIn(1L, listOf(2L)) } returns listOf(keepCardinal)
                every { clubMemberCardinalRepository.findAllByClubMembers(listOf(member)) } returns
                    listOf(keepLink, removeLink)
                every { sessionReader.findAllByClubIdAndCardinalIn(1L, listOf(8)) } returns listOf(session)
                every { attendanceRepository.findAllByClubMemberAndSessionIn(member, listOf(session)) } returns
                    emptyList()
                every { attendanceRepository.findAllByClubMemberIdAndCardinal(20L, 9) } returns emptyList()
                every { penaltyReader.countByClubMemberIdAndCardinalId(20L, 2L) } returns 0

                useCase.updateCardinals(1L, 10L, 20L, UpdateMemberCardinalRequest(cardinalIds = listOf(2L)))

                member.penaltyCount shouldBe 0
                verify(exactly = 1) { clubMemberCardinalRepository.deleteAll(listOf(removeLink)) }
            }

            it("출석/결석 기록이 있는 기수 삭제 시 force=false면 예외가 발생한다") {
                val member = createMember()
                // 현재: 8기, 9기 보유 → 요청: 9기만 유지 → 8기 삭제
                val keepCardinal = CardinalTestFixture.createCardinal(id = 2L, club = club, cardinalNumber = 9)
                val removeCardinal = CardinalTestFixture.createCardinal(id = 1L, club = club, cardinalNumber = 8)
                val keepLink = ClubMemberCardinal.create(member, keepCardinal)
                val removeLink = ClubMemberCardinal.create(member, removeCardinal)
                val session = SessionTestFixture.createSession(club = club, cardinal = 8)
                val attendance = AttendanceTestFixture.createAttendance(session, member).also { it.attend() }
                stubMemberLock(member)
                every { cardinalReader.findAllByClubIdAndIdIn(1L, listOf(2L)) } returns listOf(keepCardinal)
                every { clubMemberCardinalRepository.findAllByClubMembers(listOf(member)) } returns
                    listOf(keepLink, removeLink)
                every { sessionReader.findAllByClubIdAndCardinalIn(1L, listOf(8)) } returns listOf(session)
                every { attendanceRepository.findAllByClubMemberAndSessionIn(member, listOf(session)) } returns
                    listOf(attendance)

                shouldThrow<CardinalRemovalHasAttendanceException> {
                    useCase.updateCardinals(
                        1L,
                        10L,
                        20L,
                        UpdateMemberCardinalRequest(cardinalIds = listOf(2L), force = false),
                    )
                }

                verify(exactly = 0) {
                    attendanceRepository.deleteAll(any<List<com.weeth.domain.attendance.domain.entity.Attendance>>())
                }
                verify(exactly = 0) { clubMemberCardinalRepository.deleteAll(any()) }
            }

            it("출석/결석 기록이 있는 기수 삭제 시 force=true면 남은 출석 기록 기준으로 통계가 재계산된다") {
                val member = createMember()
                // 현재: 8기, 9기 보유 → 요청: 8기만 유지 → 9기 삭제
                val keepCardinal = CardinalTestFixture.createCardinal(id = 1L, club = club, cardinalNumber = 8)
                val removeCardinal = CardinalTestFixture.createCardinal(id = 2L, club = club, cardinalNumber = 9)
                val keepLink = ClubMemberCardinal.create(member, keepCardinal)
                val removeLink = ClubMemberCardinal.create(member, removeCardinal)
                val session8 = SessionTestFixture.createSession(club = club, cardinal = 8)
                val session9 = SessionTestFixture.createSession(club = club, cardinal = 9)
                val removeAttendance = AttendanceTestFixture.createAttendance(session9, member).also { it.attend() }
                val remainingAttendance = AttendanceTestFixture.createAttendance(session8, member).also { it.attend() }
                stubMemberLock(member)
                every { cardinalReader.findAllByClubIdAndIdIn(1L, listOf(1L)) } returns listOf(keepCardinal)
                every { clubMemberCardinalRepository.findAllByClubMembers(listOf(member)) } returns
                    listOf(keepLink, removeLink)
                every { sessionReader.findAllByClubIdAndCardinalIn(1L, listOf(9)) } returns listOf(session9)
                every { attendanceRepository.findAllByClubMemberAndSessionIn(member, listOf(session9)) } returns
                    listOf(removeAttendance)
                every { attendanceRepository.findAllByClubMemberIdAndCardinal(20L, 8) } returns
                    listOf(remainingAttendance)
                every { penaltyReader.countByClubMemberIdAndCardinalId(20L, 1L) } returns 2

                useCase.updateCardinals(
                    1L,
                    10L,
                    20L,
                    UpdateMemberCardinalRequest(cardinalIds = listOf(1L), force = true),
                )

                // 9기 제거 후 남은 출석(8기 1건) 기준으로 통계 재계산, 패널티도 8기 기준으로 복구
                member.attendanceStats.attendanceCount shouldBe 1
                member.penaltyCount shouldBe 2
                verify(exactly = 1) { attendanceRepository.deleteAll(listOf(removeAttendance)) }
                verify(exactly = 1) { clubMemberCardinalRepository.deleteAll(listOf(removeLink)) }
            }

            it("모든 기수 제거 시 출석 통계와 패널티가 0으로 초기화된다") {
                val member = createMember()
                val removeCardinal = CardinalTestFixture.createCardinal(id = 1L, club = club, cardinalNumber = 8)
                val removeLink = ClubMemberCardinal.create(member, removeCardinal)
                val session = SessionTestFixture.createSession(club = club, cardinal = 8)
                stubMemberLock(member)
                every { cardinalReader.findAllByClubIdAndIdIn(1L, emptyList()) } returns emptyList()
                every { clubMemberCardinalRepository.findAllByClubMembers(listOf(member)) } returns listOf(removeLink)
                every { sessionReader.findAllByClubIdAndCardinalIn(1L, listOf(8)) } returns listOf(session)
                every { attendanceRepository.findAllByClubMemberAndSessionIn(member, listOf(session)) } returns
                    emptyList()

                useCase.updateCardinals(1L, 10L, 20L, UpdateMemberCardinalRequest(cardinalIds = emptyList()))

                member.attendanceStats.attendanceCount shouldBe 0
                member.attendanceStats.absenceCount shouldBe 0
                member.penaltyCount shouldBe 0
            }

            it("존재하지 않는 기수 ID가 포함되면 예외가 발생한다") {
                val member = createMember()
                stubMemberLock(member)
                every { cardinalReader.findAllByClubIdAndIdIn(1L, listOf(999L)) } returns emptyList()

                shouldThrow<CardinalNotFoundException> {
                    useCase.updateCardinals(1L, 10L, 20L, UpdateMemberCardinalRequest(cardinalIds = listOf(999L)))
                }
            }

            it("다른 동아리 소속 멤버면 예외가 발생한다") {
                val otherClubMember = ClubMemberTestFixture.createActiveMember(id = 20L)
                every { clubPermissionPolicy.requireAdmin(1L, 10L) } returns adminMember
                every { clubMemberReader.findByIdWithLock(20L) } returns otherClubMember

                shouldThrow<ClubMemberNotInClubException> {
                    useCase.updateCardinals(1L, 10L, 20L, UpdateMemberCardinalRequest(cardinalIds = listOf(1L)))
                }
            }
        }
    })
