package com.weeth.domain.attendance.application.usecase.query

import com.weeth.domain.attendance.application.exception.AttendanceNotFoundException
import com.weeth.domain.attendance.application.mapper.AttendanceMapper
import com.weeth.domain.attendance.domain.entity.Attendance
import com.weeth.domain.attendance.domain.enums.AttendanceStatus
import com.weeth.domain.attendance.domain.repository.AttendanceRepository
import com.weeth.domain.cardinal.application.exception.CardinalNotFoundException
import com.weeth.domain.cardinal.domain.repository.CardinalReader
import com.weeth.domain.cardinal.fixture.CardinalTestFixture
import com.weeth.domain.club.domain.service.ClubMemberCardinalPolicy
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.club.domain.service.ClubPermissionPolicy
import com.weeth.domain.club.fixture.ClubMemberTestFixture
import com.weeth.domain.session.domain.repository.SessionReader
import com.weeth.domain.session.fixture.SessionTestFixture
import com.weeth.domain.user.fixture.UserTestFixture
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
        val cardinalReader = mockk<CardinalReader>()

        val queryService =
            GetAttendanceQueryService(
                clubMemberPolicy,
                clubPermissionPolicy,
                clubMemberCardinalPolicy,
                sessionReader,
                attendanceRepository,
                attendanceMapper,
                cardinalReader,
            )

        beforeTest {
            clearMocks(
                clubMemberPolicy,
                clubPermissionPolicy,
                clubMemberCardinalPolicy,
                sessionReader,
                attendanceRepository,
                cardinalReader,
            )
        }

        describe("선택 기수 출석") {
            it("다른 기수 누적값을 쓰지 않고 ATTEND와 ABSENT만 집계한다") {
                val member = ClubMemberTestFixture.createActiveMember()
                repeat(9) { member.attend() }
                val cardinal = CardinalTestFixture.createCardinal(club = member.club, cardinalNumber = 7)
                val session = SessionTestFixture.createSession(club = member.club, cardinal = 7)
                val records =
                    listOf(
                        Attendance.create(session, member).also { it.attend() },
                        Attendance.create(session, member).also { it.absent() },
                        Attendance.create(session, member),
                    )
                every { clubMemberPolicy.getActiveMember(member.club.id, member.user.id) } returns member
                every { cardinalReader.findByClubIdAndCardinalNumber(member.club.id, 7) } returns cardinal
                every { attendanceRepository.findAllByClubMemberIdAndCardinal(member.id, 7) } returns records

                val result = queryService.findAllDetailsByCurrentCardinal(member.club.id, member.user.id, 7)

                result.cardinalNumber shouldBe 7
                result.attendanceCount shouldBe 1
                result.absenceCount shouldBe 1
                result.total shouldBe 2
                result.attendanceRate shouldBe 50
                result.attendances shouldHaveSize 3
                member.attendanceStats.attendanceCount shouldBe 9
                verify(exactly = 0) { clubMemberCardinalPolicy.getCurrentCardinal(any()) }
            }

            it("존재하는 기수에 내 기록이 없으면 빈 목록과 0퍼센트이다") {
                val member = ClubMemberTestFixture.createActiveMember()
                every { clubMemberPolicy.getActiveMember(member.club.id, member.user.id) } returns member
                every { cardinalReader.findByClubIdAndCardinalNumber(member.club.id, 7) } returns
                    CardinalTestFixture.createCardinal(club = member.club, cardinalNumber = 7)
                every { attendanceRepository.findAllByClubMemberIdAndCardinal(member.id, 7) } returns emptyList()

                val result = queryService.findAllDetailsByCurrentCardinal(member.club.id, member.user.id, 7)
                result.total shouldBe 0
                result.attendanceRate shouldBe 0
                result.attendances shouldHaveSize 0
            }

            it("동아리에 존재하지 않는 기수이면 조회하지 않고 404 도메인 예외이다") {
                val member = ClubMemberTestFixture.createActiveMember()
                every { clubMemberPolicy.getActiveMember(member.club.id, member.user.id) } returns member
                every { cardinalReader.findByClubIdAndCardinalNumber(member.club.id, 999) } returns null

                shouldThrow<CardinalNotFoundException> {
                    queryService.findAllDetailsByCurrentCardinal(member.club.id, member.user.id, 999)
                }
                verify(exactly = 0) { attendanceRepository.findAllByClubMemberIdAndCardinal(any(), any()) }
            }

            it("소속 기수 없는 멤버의 기수 생략 상세는 기존 기수 없음 예외를 유지한다") {
                val member = ClubMemberTestFixture.createActiveMember()
                every { clubMemberPolicy.getActiveMember(member.club.id, member.user.id) } returns member
                every { clubMemberCardinalPolicy.getCurrentCardinal(member) } throws CardinalNotFoundException()
                shouldThrow<CardinalNotFoundException> {
                    queryService.findAllDetailsByCurrentCardinal(member.club.id, member.user.id)
                }
                verify(exactly = 0) { attendanceRepository.findAllByClubMemberIdAndCardinal(any(), any()) }
            }
        }

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
                val attendances =
                    listOf(
                        Attendance.create(session1, member).also { it.attend() },
                        Attendance.create(session2, member),
                    )

                every { clubMemberPolicy.getActiveMember(member.club.id, member.user.id) } returns member
                every { clubMemberCardinalPolicy.getCurrentCardinal(member) } returns cardinal
                every { attendanceRepository.findAllByClubMemberIdAndCardinal(member.id, 8) } returns attendances

                val result = queryService.findAllDetailsByCurrentCardinal(member.club.id, member.user.id)

                result.attendanceCount shouldBe 1
                result.absenceCount shouldBe 0
                result.total shouldBe 1
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
                every { attendanceRepository.findAllBySession(session) } returns listOf(attendance)

                val result = queryService.findAllAttendanceBySession(admin.club.id, admin.user.id, session.id)

                result shouldHaveSize 1
                result.first().name shouldBe member.user.name
                result.first().memberStatus shouldBe member.memberStatus
                result.first().status shouldBe AttendanceStatus.ATTEND
            }

            it("관리자는 LEFT 멤버의 세션별 출석도 함께 조회한다") {
                val admin = ClubMemberTestFixture.createAdminMember()
                val activeMember = ClubMemberTestFixture.createActiveMember(club = admin.club)
                val leftMember =
                    ClubMemberTestFixture
                        .createActiveMember(club = admin.club, user = UserTestFixture.createActiveUser2())
                        .also { it.leave(LocalDateTime.of(2026, 5, 19, 12, 0)) }
                val session = SessionTestFixture.createSession(id = 10L, club = admin.club, title = "세션")
                val activeAttendance = Attendance.create(session, activeMember).also { it.attend() }
                val leftAttendance = Attendance.create(session, leftMember).also { it.absent() }

                every { clubPermissionPolicy.requireAdmin(admin.club.id, admin.user.id) } returns admin
                every { sessionReader.getById(session.id) } returns session
                every { attendanceRepository.findAllBySession(session) } returns
                    listOf(activeAttendance, leftAttendance)

                val result = queryService.findAllAttendanceBySession(admin.club.id, admin.user.id, session.id)

                result shouldHaveSize 2
                result.map { it.name } shouldBe listOf(activeMember.user.name, leftMember.user.name)
                result.map { it.memberStatus } shouldBe listOf(activeMember.memberStatus, leftMember.memberStatus)
                result.map { it.status } shouldBe listOf(AttendanceStatus.ATTEND, AttendanceStatus.ABSENT)
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
