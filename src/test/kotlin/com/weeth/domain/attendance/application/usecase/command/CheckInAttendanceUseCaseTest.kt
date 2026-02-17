package com.weeth.domain.attendance.application.usecase.command

import com.weeth.domain.attendance.application.exception.AttendanceCodeMismatchException
import com.weeth.domain.attendance.application.exception.AttendanceNotFoundException
import com.weeth.domain.attendance.domain.entity.Attendance
import com.weeth.domain.attendance.domain.entity.enums.Status
import com.weeth.domain.attendance.fixture.AttendanceTestFixture.createActiveUserWithAttendances
import com.weeth.domain.attendance.fixture.AttendanceTestFixture.createInProgressMeeting
import com.weeth.domain.attendance.fixture.AttendanceTestFixture.setUserAttendanceStats
import com.weeth.domain.schedule.domain.entity.Meeting
import com.weeth.domain.user.domain.entity.User
import com.weeth.domain.user.domain.service.UserGetService
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime

class CheckInAttendanceUseCaseTest :
    DescribeSpec({

        val userId = 10L
        val userGetService = mockk<UserGetService>()

        val useCase = CheckInAttendanceUseCase(userGetService)

        describe("execute") {
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
                    setUserAttendanceStats(user, 0, 0)

                    every { userGetService.find(userId) } returns user

                    shouldNotThrowAny {
                        useCase.execute(userId, 1234)
                    }
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
                        useCase.execute(userId, 1234)
                    }
                }
            }

            context("진행 중 정기모임이고 코드 일치하며 상태가 ATTEND가 아닐 때") {
                it("출석 처리된다") {
                    val user = mockk<User>()
                    val inProgressMeeting = createInProgressMeeting(1, 1234, "InProgress")
                    val attendance = mockk<Attendance>(relaxUnitFun = true)
                    every { attendance.meeting } returns inProgressMeeting
                    every { attendance.isWrong(1234) } returns false
                    every { attendance.status } returns Status.PENDING

                    every { userGetService.find(userId) } returns user
                    every { user.attendances } returns listOf(attendance)
                    every { user.attend() } returns Unit

                    useCase.execute(userId, 1234)

                    verify { attendance.attend() }
                    verify { user.attend() }
                }
            }

            context("진행 중 정기모임이 없을 때") {
                it("AttendanceNotFoundException") {
                    val user = mockk<User>()
                    every { userGetService.find(userId) } returns user
                    every { user.attendances } returns listOf()

                    shouldThrow<AttendanceNotFoundException> {
                        useCase.execute(userId, 1234)
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
                        useCase.execute(userId, 9999)
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

                    useCase.execute(userId, 1234)

                    verify(exactly = 0) { attendance.attend() }
                    verify(exactly = 0) { user.attend() }
                }
            }
        }
    })
