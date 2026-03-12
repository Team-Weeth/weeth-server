package com.weeth.domain.attendance.application.usecase.command

import com.weeth.domain.attendance.application.dto.request.UpdateAttendanceStatusRequest
import com.weeth.domain.attendance.application.exception.AlreadyAttendedException
import com.weeth.domain.attendance.application.exception.AttendanceNotFoundException
import com.weeth.domain.attendance.domain.port.QrAttendancePort
import com.weeth.domain.attendance.domain.repository.AttendanceRepository
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.club.fixture.ClubMemberTestFixture
import com.weeth.domain.session.domain.repository.SessionReader
import com.weeth.domain.session.fixture.SessionTestFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk

class ManageAttendanceUseCaseTest :
    DescribeSpec({
        val clubMemberPolicy = mockk<ClubMemberPolicy>()
        val sessionReader = mockk<SessionReader>()
        val attendanceRepository = mockk<AttendanceRepository>()
        val qrAttendancePort = mockk<QrAttendancePort>()

        val useCase =
            ManageAttendanceUseCase(
                clubMemberPolicy,
                sessionReader,
                attendanceRepository,
                qrAttendancePort,
            )

        beforeTest {
            clearMocks(clubMemberPolicy, sessionReader, attendanceRepository, qrAttendancePort)
        }

        describe("checkIn") {
            val clubMember = ClubMemberTestFixture.createActiveMember()
            val session = SessionTestFixture.createInProgressSession(cardinal = 1, code = 123456, title = "Test Session", club = clubMember.club)
            val attendance = com.weeth.domain.attendance.domain.entity.Attendance.create(session, clubMember)

            it("정상 체크인 시 출석 상태와 멤버 통계를 갱신한다") {
                every { qrAttendancePort.getCode(session.id) } returns session.code
                every { sessionReader.getById(session.id) } returns session
                every { clubMemberPolicy.getActiveMember(clubMember.club.id, clubMember.user.id) } returns clubMember
                every { attendanceRepository.findBySessionAndClubMemberWithLock(session, clubMember) } returns attendance

                useCase.checkIn(clubMember.club.id, clubMember.user.id, session.id, session.code)

                attendance.status shouldBe com.weeth.domain.attendance.domain.enums.AttendanceStatus.ATTEND
                clubMember.attendanceStats.attendanceCount shouldBe 1
            }

            it("이미 출석 처리된 경우 예외를 던진다") {
                val attendedAttendance = com.weeth.domain.attendance.domain.entity.Attendance.create(session, clubMember).also { it.attend() }
                every { qrAttendancePort.getCode(session.id) } returns session.code
                every { sessionReader.getById(session.id) } returns session
                every { clubMemberPolicy.getActiveMember(clubMember.club.id, clubMember.user.id) } returns clubMember
                every { attendanceRepository.findBySessionAndClubMemberWithLock(session, clubMember) } returns attendedAttendance

                shouldThrow<AlreadyAttendedException> {
                    useCase.checkIn(clubMember.club.id, clubMember.user.id, session.id, session.code)
                }
            }

            it("출석 레코드가 없으면 예외를 던진다") {
                every { qrAttendancePort.getCode(session.id) } returns session.code
                every { sessionReader.getById(session.id) } returns session
                every { clubMemberPolicy.getActiveMember(clubMember.club.id, clubMember.user.id) } returns clubMember
                every { attendanceRepository.findBySessionAndClubMemberWithLock(session, clubMember) } returns null

                shouldThrow<AttendanceNotFoundException> {
                    useCase.checkIn(clubMember.club.id, clubMember.user.id, session.id, session.code)
                }
            }
        }

        describe("updateStatus") {
            it("관리자가 ATTEND로 변경하면 ClubMember 통계를 갱신한다") {
                val admin = ClubMemberTestFixture.createAdminMember()
                val member = ClubMemberTestFixture.createActiveMember(club = admin.club)
                val attendance = com.weeth.domain.attendance.domain.entity.Attendance.create(SessionTestFixture.createSession(club = admin.club), member)

                every { clubMemberPolicy.requireAdmin(admin.club.id, admin.user.id) } returns admin
                every { attendanceRepository.findByIdWithClubMember(1L) } returns attendance

                useCase.updateStatus(admin.club.id, admin.user.id, listOf(UpdateAttendanceStatusRequest(1L, "ATTEND")))

                attendance.status shouldBe com.weeth.domain.attendance.domain.enums.AttendanceStatus.ATTEND
                member.attendanceStats.attendanceCount shouldBe 1
            }

            it("관리자가 PENDING으로 되돌리면 기존 통계를 차감한다") {
                val admin = ClubMemberTestFixture.createAdminMember()
                val member = ClubMemberTestFixture.createActiveMember(club = admin.club)
                val attendance = com.weeth.domain.attendance.domain.entity.Attendance.create(SessionTestFixture.createSession(club = admin.club), member)
                attendance.attend()
                member.attend()

                every { clubMemberPolicy.requireAdmin(admin.club.id, admin.user.id) } returns admin
                every { attendanceRepository.findByIdWithClubMember(1L) } returns attendance

                useCase.updateStatus(admin.club.id, admin.user.id, listOf(UpdateAttendanceStatusRequest(1L, "PENDING")))

                attendance.status shouldBe com.weeth.domain.attendance.domain.enums.AttendanceStatus.PENDING
                member.attendanceStats.attendanceCount shouldBe 0
            }
        }
    })
