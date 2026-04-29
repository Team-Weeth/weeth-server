package com.weeth.domain.attendance.application.usecase.query

import com.weeth.domain.attendance.application.exception.AttendanceNotFoundException
import com.weeth.domain.attendance.application.mapper.AttendanceMapper
import com.weeth.domain.attendance.domain.entity.Attendance
import com.weeth.domain.attendance.domain.enums.AttendanceStatus
import com.weeth.domain.attendance.domain.repository.AttendanceRepository
import com.weeth.domain.cardinal.fixture.CardinalTestFixture
import com.weeth.domain.club.domain.service.ClubMemberCardinalPolicy
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.club.domain.service.ClubPermissionPolicy
import com.weeth.domain.club.fixture.ClubMemberTestFixture
import com.weeth.domain.session.domain.repository.SessionReader
import com.weeth.domain.session.fixture.SessionTestFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime

class GetAttendanceQueryServiceTest :
    DescribeSpec({
        val clubMemberPolicy = mockk<ClubMemberPolicy>()
        val clubPermissionPolicy = mockk<ClubPermissionPolicy>()
        val clubMemberCardinalPolicy = mockk<ClubMemberCardinalPolicy>()
        val sessionReader = mockk<SessionReader>()
        val attendanceRepository = mockk<AttendanceRepository>()
        val attendanceMapper = AttendanceMapper()

        val queryService =
            GetAttendanceQueryService(
                clubMemberPolicy,
                clubPermissionPolicy,
                clubMemberCardinalPolicy,
                sessionReader,
                attendanceRepository,
                attendanceMapper,
            )

        describe("findAttendance") {
            beforeTest {
                clearMocks(clubMemberPolicy, attendanceRepository)
            }

            it("오늘 출석이 1개이면 해당 출석을 반환한다") {
                val member = ClubMemberTestFixture.createActiveMember()
                member.attend()
                val session =
                    SessionTestFixture.createInProgressSession(
                        cardinal = 1,
                        code = 111111,
                        title = "오늘 모임",
                        club = member.club,
                    )
                val attendance = Attendance.create(session, member)

                every { clubMemberPolicy.getActiveMember(member.club.id, member.user.id) } returns member
                every { attendanceRepository.findTodayByClubMemberId(member.id, any(), any()) } returns
                    listOf(attendance)

                val result = queryService.findAttendance(member.club.id, member.user.id)

                result.attendanceRate shouldBe member.attendanceStats.attendanceRate
                result.title shouldBe session.title
                result.status shouldBe AttendanceStatus.PENDING
                verify(exactly = 1) { clubMemberPolicy.getActiveMember(member.club.id, member.user.id) }
            }

            it("오늘 출석이 없으면 세션 관련 필드를 null로 반환한다") {
                val member = ClubMemberTestFixture.createActiveMember()

                every { clubMemberPolicy.getActiveMember(member.club.id, member.user.id) } returns member
                every { attendanceRepository.findTodayByClubMemberId(member.id, any(), any()) } returns emptyList()

                val result = queryService.findAttendance(member.club.id, member.user.id)

                result.title shouldBe null
                result.status shouldBe null
                result.sessionId shouldBe null
            }

            it("오늘 세션이 여러 개이면 현재 시각 이후 가장 가까운 세션을 반환한다") {
                val member = ClubMemberTestFixture.createActiveMember()
                val now = LocalDateTime.now()
                val pastSession =
                    SessionTestFixture.createSession(
                        title = "오전 세션",
                        start = now.minusHours(3),
                        end = now.minusHours(1),
                        club = member.club,
                    )
                val upcomingSession =
                    SessionTestFixture.createSession(
                        title = "오후 세션",
                        start = now.plusHours(1),
                        end = now.plusHours(3),
                        club = member.club,
                    )

                every { clubMemberPolicy.getActiveMember(member.club.id, member.user.id) } returns member
                every { attendanceRepository.findTodayByClubMemberId(member.id, any(), any()) } returns
                    listOf(
                        Attendance.create(pastSession, member),
                        Attendance.create(upcomingSession, member),
                    )

                val result = queryService.findAttendance(member.club.id, member.user.id)

                result.title shouldBe "오후 세션"
            }

            it("오늘 세션이 여러 개이고 모두 현재 시각 이전이면 마지막 세션을 반환한다") {
                val member = ClubMemberTestFixture.createActiveMember()
                val now = LocalDateTime.now()
                val morningSession =
                    SessionTestFixture.createSession(
                        title = "오전 세션",
                        start = now.minusHours(5),
                        end = now.minusHours(3),
                        club = member.club,
                    )
                val afternoonSession =
                    SessionTestFixture.createSession(
                        title = "오후 세션",
                        start = now.minusHours(2),
                        end = now.minusHours(1),
                        club = member.club,
                    )

                every { clubMemberPolicy.getActiveMember(member.club.id, member.user.id) } returns member
                every { attendanceRepository.findTodayByClubMemberId(member.id, any(), any()) } returns
                    listOf(
                        Attendance.create(morningSession, member),
                        Attendance.create(afternoonSession, member),
                    )

                val result = queryService.findAttendance(member.club.id, member.user.id)

                result.title shouldBe "오후 세션"
            }
        }

        describe("findAllDetailsByCurrentCardinal") {
            it("현재 기수의 출석 상세 목록과 통계를 반환한다") {
                val member = ClubMemberTestFixture.createActiveMember()
                repeat(2) { member.attend() }
                repeat(1) { member.absent() }
                val cardinal =
                    CardinalTestFixture.createCardinal(
                        id = 1L,
                        club = member.club,
                        cardinalNumber = 8,
                    )
                val session1 =
                    SessionTestFixture.createSession(
                        id = 1L,
                        club = member.club,
                        cardinal = 8,
                        title = "1주차",
                    )
                val session2 =
                    SessionTestFixture.createSession(
                        id = 2L,
                        club = member.club,
                        cardinal = 8,
                        title = "2주차",
                    )
                val attendances = listOf(Attendance.create(session1, member), Attendance.create(session2, member))

                every { clubMemberPolicy.getActiveMember(member.club.id, member.user.id) } returns member
                every { clubMemberCardinalPolicy.getCurrentCardinal(member) } returns cardinal
                every { attendanceRepository.findAllByClubMemberIdAndCardinal(member.id, 8) } returns attendances

                val result = queryService.findAllDetailsByCurrentCardinal(member.club.id, member.user.id)

                result.attendanceCount shouldBe 2
                result.absenceCount shouldBe 1
                result.total shouldBe 3
                result.attendances shouldHaveSize 2
                result.attendances.map { it.title } shouldBe listOf("1주차", "2주차")
            }
        }

        describe("findAllAttendanceBySession") {
            it("관리자는 세션별 출석 목록을 조회할 수 있다") {
                val admin = ClubMemberTestFixture.createAdminMember()
                val member = ClubMemberTestFixture.createActiveMember(club = admin.club)
                val session = SessionTestFixture.createSession(id = 10L, club = admin.club, title = "세션")
                val attendance = Attendance.create(session, member).also { it.attend() }

                every { clubPermissionPolicy.requireAdmin(admin.club.id, admin.user.id) } returns admin
                every { sessionReader.getById(session.id) } returns session
                every { attendanceRepository.findAllBySessionAndClubMemberMemberStatus(session, any()) } returns
                    listOf(attendance)

                val result = queryService.findAllAttendanceBySession(admin.club.id, admin.user.id, session.id)

                result shouldHaveSize 1
                result.first().name shouldBe member.user.name
                result.first().status shouldBe AttendanceStatus.ATTEND
            }

            it("다른 동아리 세션이면 예외를 던진다") {
                val admin = ClubMemberTestFixture.createAdminMember()
                val otherSession = SessionTestFixture.createSession(id = 10L)

                every { clubPermissionPolicy.requireAdmin(admin.club.id, admin.user.id) } returns admin
                every { sessionReader.getById(otherSession.id) } returns otherSession

                shouldThrow<AttendanceNotFoundException> {
                    queryService.findAllAttendanceBySession(admin.club.id, admin.user.id, otherSession.id)
                }
            }
        }
    })
