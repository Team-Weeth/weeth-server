package com.weeth.domain.attendance.application.usecase

import com.weeth.domain.attendance.application.dto.AttendanceDTO
import com.weeth.domain.attendance.application.exception.AttendanceCodeMismatchException
import com.weeth.domain.attendance.application.exception.AttendanceNotFoundException
import com.weeth.domain.attendance.application.mapper.AttendanceMapper
import com.weeth.domain.attendance.domain.entity.Attendance
import com.weeth.domain.attendance.domain.entity.enums.Status
import com.weeth.domain.attendance.domain.service.AttendanceGetService
import com.weeth.domain.attendance.domain.service.AttendanceUpdateService
import com.weeth.domain.attendance.fixture.AttendanceTestFixture.createActiveUserWithAttendances
import com.weeth.domain.attendance.fixture.AttendanceTestFixture.createInProgressMeeting
import com.weeth.domain.attendance.fixture.AttendanceTestFixture.createOneDayMeeting
import com.weeth.domain.schedule.application.exception.MeetingNotFoundException
import com.weeth.domain.schedule.domain.entity.Meeting
import com.weeth.domain.schedule.domain.service.MeetingGetService
import com.weeth.domain.user.domain.entity.Cardinal
import com.weeth.domain.user.domain.entity.User
import com.weeth.domain.user.domain.service.UserCardinalGetService
import com.weeth.domain.user.domain.service.UserGetService
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import java.time.LocalDateTime

class AttendanceUseCaseImplTest :
    DescribeSpec({

        val userId = 10L
        val userGetService = mockk<UserGetService>()
        val userCardinalGetService = mockk<UserCardinalGetService>()
        val attendanceGetService = mockk<AttendanceGetService>()
        val attendanceUpdateService = mockk<AttendanceUpdateService>(relaxUnitFun = true)
        val attendanceMapper = mockk<AttendanceMapper>()
        val meetingGetService = mockk<MeetingGetService>()

        val attendanceUseCase =
            AttendanceUseCaseImpl(
                userGetService,
                userCardinalGetService,
                attendanceGetService,
                attendanceUpdateService,
                attendanceMapper,
                meetingGetService,
            )

        describe("find") {
            it("여러 날짜의 출석 목록 중 시작/종료 날짜가 모두 오늘인 출석정보를 선택") {
                val today = LocalDate.now()

                val meetingYesterday = createOneDayMeeting(today.minusDays(1), 1, 1111, "Yesterday")
                val meetingToday = createOneDayMeeting(today, 1, 2222, "Today")
                val meetingTomorrow = createOneDayMeeting(today.plusDays(1), 1, 3333, "Tomorrow")

                val user =
                    createActiveUserWithAttendances(
                        "이지훈",
                        listOf(meetingYesterday, meetingToday, meetingTomorrow),
                    )

                val expectedTodayAttendance =
                    user.attendances.first {
                        it.meeting.title == "Today"
                    }

                val mapped = mockk<AttendanceDTO.Main>()

                every { userGetService.find(userId) } returns user
                every { attendanceMapper.toMainDto(eq(user), eq(expectedTodayAttendance)) } returns mapped

                val actual = attendanceUseCase.find(userId)

                actual shouldBe mapped
                verify { attendanceMapper.toMainDto(eq(user), eq(expectedTodayAttendance)) }
            }

            it("시작/종료 날짜가 모두 오늘인 출석이 없다면 mapper.toMainDto(user, null)을 호출") {
                val today = LocalDate.now()

                val yesterdayMeeting = createOneDayMeeting(today.minusDays(1), 1, 1111, "Yesterday")
                val tomorrowMeeting = createOneDayMeeting(today.plusDays(1), 1, 3333, "Tomorrow")

                val user =
                    createActiveUserWithAttendances(
                        "이지훈",
                        listOf(yesterdayMeeting, tomorrowMeeting),
                    )

                val mapped = mockk<AttendanceDTO.Main>()
                every { userGetService.find(userId) } returns user
                every { attendanceMapper.toMainDto(user, null) } returns mapped

                val actual = attendanceUseCase.find(userId)

                actual shouldBe mapped
                verify { attendanceMapper.toMainDto(user, null) }
            }
        }

        describe("checkIn") {
            context("10분 전부터 출석이 가능한지 확인") {
                it("5분 뒤 시작 회의에 출석 성공") {
                    val now = LocalDateTime.now()
                    val meeting =
                        Meeting
                            .builder()
                            .start(now.plusMinutes(5))
                            .end(now.plusHours(2))
                            .code(1234)
                            .title("Today")
                            .cardinal(1)
                            .build()

                    val user = createActiveUserWithAttendances("이지훈", listOf(meeting))

                    every { userGetService.find(userId) } returns user

                    shouldNotThrowAny {
                        attendanceUseCase.checkIn(userId, 1234)
                    }
                    verify(exactly = 1) { attendanceUpdateService.attend(any<Attendance>()) }
                }

                it("11분 전에 출석시 오류 발생") {
                    val now = LocalDateTime.now()
                    val meeting =
                        Meeting
                            .builder()
                            .start(now.plusMinutes(11))
                            .end(now.plusHours(2))
                            .code(1234)
                            .title("Today")
                            .cardinal(1)
                            .build()

                    val user = createActiveUserWithAttendances("이지훈", listOf(meeting))

                    every { userGetService.find(userId) } returns user

                    shouldThrow<AttendanceNotFoundException> {
                        attendanceUseCase.checkIn(userId, 1234)
                    }
                }
            }

            context("진행 중 정기모임이고 코드 일치하며 상태가 ATTEND가 아닐 때") {
                it("출석 처리된다") {
                    val user = mockk<User>()
                    val inProgressMeeting = createInProgressMeeting(1, 1234, "InProgress")
                    val attendance = mockk<Attendance>()
                    every { attendance.meeting } returns inProgressMeeting
                    every { attendance.isWrong(1234) } returns false
                    every { attendance.status } returns Status.PENDING

                    every { userGetService.find(userId) } returns user
                    every { user.attendances } returns listOf(attendance)

                    attendanceUseCase.checkIn(userId, 1234)

                    verify { attendanceUpdateService.attend(attendance) }
                }
            }

            context("진행 중 정기모임이 없을 때") {
                it("AttendanceNotFoundException") {
                    val user = mockk<User>()
                    every { userGetService.find(userId) } returns user
                    every { user.attendances } returns listOf()

                    shouldThrow<AttendanceNotFoundException> {
                        attendanceUseCase.checkIn(userId, 1234)
                    }
                }
            }

            context("코드 불일치 시") {
                it("AttendanceCodeMismatchException") {
                    val user = mockk<User>()
                    val inProgressMeeting = createInProgressMeeting(1, 1234, "InProgress")

                    val attendance = mockk<Attendance>()
                    every { attendance.meeting } returns inProgressMeeting
                    every { attendance.isWrong(9999) } returns true

                    every { userGetService.find(userId) } returns user
                    every { user.attendances } returns listOf(attendance)

                    shouldThrow<AttendanceCodeMismatchException> {
                        attendanceUseCase.checkIn(userId, 9999)
                    }
                }
            }

            context("이미 ATTEND일 때") {
                it("추가 처리 없이 종료") {
                    val user = mockk<User>()
                    val inProgressMeeting = createInProgressMeeting(1, 1234, "InProgress")

                    val attendance = mockk<Attendance>()
                    every { attendance.meeting } returns inProgressMeeting
                    every { attendance.isWrong(1234) } returns false
                    every { attendance.status } returns Status.ATTEND

                    every { userGetService.find(userId) } returns user
                    every { user.attendances } returns listOf(attendance)

                    attendanceUseCase.checkIn(userId, 1234)

                    verify(exactly = 0) { attendanceUpdateService.attend(attendance) }
                }
            }
        }

        describe("findAllDetailsByCurrentCardinal") {
            it("현재 기수만 필터링·정렬하여 Detail 매핑") {
                val today = LocalDate.now()
                val meetingDayMinus1 = createOneDayMeeting(today.minusDays(1), 1, 1111, "D-1")
                val meetingToday = createOneDayMeeting(today, 1, 2222, "D-Day")
                val user = createActiveUserWithAttendances("이지훈", listOf(meetingDayMinus1, meetingToday))

                val userAttendances = user.attendances
                val attendanceFirst = userAttendances[0]
                val attendanceSecond = userAttendances[1]

                every { userGetService.find(userId) } returns user
                val currentCardinal = mockk<Cardinal>()
                every { currentCardinal.cardinalNumber } returns 1
                every { userCardinalGetService.getCurrentCardinal(user) } returns currentCardinal

                val responseFirst = mockk<AttendanceDTO.Response>()
                val responseSecond = mockk<AttendanceDTO.Response>()
                every { attendanceMapper.toResponseDto(attendanceFirst) } returns responseFirst
                every { attendanceMapper.toResponseDto(attendanceSecond) } returns responseSecond

                val expectedDetail = mockk<AttendanceDTO.Detail>()
                every { attendanceMapper.toDetailDto(eq(user), any()) } returns expectedDetail

                val actualDetail = attendanceUseCase.findAllDetailsByCurrentCardinal(userId)

                actualDetail shouldBe expectedDetail
                verify {
                    attendanceMapper.toDetailDto(
                        eq(user),
                        match { it.size == 2 },
                    )
                }
            }
        }

        describe("close") {
            it("당일 정기모임을 찾아 close") {
                val now = LocalDate.now()
                val targetMeeting = createOneDayMeeting(now, 1, 1111, "Today")
                val otherMeeting = createOneDayMeeting(now.minusDays(1), 1, 9999, "Yesterday")

                val attendance1 = mockk<Attendance>()
                val attendance2 = mockk<Attendance>()

                every { meetingGetService.find(1) } returns listOf(targetMeeting, otherMeeting)
                every { attendanceGetService.findAllByMeeting(targetMeeting) } returns listOf(attendance1, attendance2)

                attendanceUseCase.close(now, 1)

                verify {
                    attendanceUpdateService.close(
                        match { it.size == 2 && it.containsAll(listOf(attendance1, attendance2)) },
                    )
                }
            }

            it("당일 정기모임이 없으면 MeetingNotFoundException") {
                val now = LocalDate.now()
                val otherDayMeeting = createOneDayMeeting(now.minusDays(1), 1, 9999, "Yesterday")

                every { meetingGetService.find(1) } returns listOf(otherDayMeeting)

                shouldThrow<MeetingNotFoundException> {
                    attendanceUseCase.close(now, 1)
                }
            }
        }
    })
