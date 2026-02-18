package com.weeth.domain.attendance.application.usecase.command

import com.weeth.domain.attendance.application.exception.AttendanceCodeMismatchException
import com.weeth.domain.attendance.application.exception.AttendanceNotFoundException
import com.weeth.domain.attendance.domain.entity.Attendance
import com.weeth.domain.attendance.domain.enums.Status
import com.weeth.domain.attendance.domain.repository.AttendanceRepository
import com.weeth.domain.user.domain.entity.User
import com.weeth.domain.user.domain.service.UserGetService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class CheckInAttendanceUseCaseTest :
    DescribeSpec({

        val userId = 10L
        val userGetService = mockk<UserGetService>()
        val attendanceRepository = mockk<AttendanceRepository>()

        val useCase = CheckInAttendanceUseCase(userGetService, attendanceRepository)

        describe("checkIn") {
            context("진행 중 정기모임이고 코드 일치하며 상태가 ATTEND가 아닐 때") {
                it("출석 처리된다") {
                    val user = mockk<User>()
                    val attendance = mockk<Attendance>(relaxUnitFun = true)
                    every { attendance.isWrong(1234) } returns false
                    every { attendance.status } returns Status.PENDING

                    every { userGetService.find(userId) } returns user
                    every { attendanceRepository.findCurrentByUserId(eq(userId), any(), any()) } returns attendance
                    every { user.attend() } returns Unit

                    useCase.checkIn(userId, 1234)

                    verify { attendance.attend() }
                    verify { user.attend() }
                }
            }

            context("진행 중 정기모임이 없을 때") {
                it("AttendanceNotFoundException") {
                    val user = mockk<User>()
                    every { userGetService.find(userId) } returns user
                    every { attendanceRepository.findCurrentByUserId(eq(userId), any(), any()) } returns null

                    shouldThrow<AttendanceNotFoundException> {
                        useCase.checkIn(userId, 1234)
                    }
                }
            }

            context("코드 불일치 시") {
                it("AttendanceCodeMismatchException") {
                    val user = mockk<User>()
                    val attendance = mockk<Attendance>()
                    every { attendance.isWrong(9999) } returns true

                    every { userGetService.find(userId) } returns user
                    every { attendanceRepository.findCurrentByUserId(eq(userId), any(), any()) } returns attendance

                    shouldThrow<AttendanceCodeMismatchException> {
                        useCase.checkIn(userId, 9999)
                    }
                }
            }

            context("이미 ATTEND일 때") {
                it("추가 처리 없이 종료") {
                    val user = mockk<User>()
                    val attendance = mockk<Attendance>()
                    every { attendance.isWrong(1234) } returns false
                    every { attendance.status } returns Status.ATTEND

                    every { userGetService.find(userId) } returns user
                    every { attendanceRepository.findCurrentByUserId(eq(userId), any(), any()) } returns attendance

                    useCase.checkIn(userId, 1234)

                    verify(exactly = 0) { attendance.attend() }
                    verify(exactly = 0) { user.attend() }
                }
            }
        }
    })
