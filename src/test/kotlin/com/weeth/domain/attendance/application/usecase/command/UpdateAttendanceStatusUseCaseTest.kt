package com.weeth.domain.attendance.application.usecase.command

import com.weeth.domain.attendance.application.dto.request.UpdateAttendanceStatusRequest
import com.weeth.domain.attendance.application.exception.AttendanceNotFoundException
import com.weeth.domain.attendance.domain.entity.Attendance
import com.weeth.domain.attendance.domain.repository.AttendanceRepository
import com.weeth.domain.user.domain.entity.User
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.repository.findByIdOrNull

class UpdateAttendanceStatusUseCaseTest :
    DescribeSpec({

        val attendanceRepository = mockk<AttendanceRepository>()

        val useCase = UpdateAttendanceStatusUseCase(attendanceRepository)

        describe("execute") {
            context("ABSENT로 변경 시") {
                it("close + removeAttend + absent 호출") {
                    val user = mockk<User>(relaxUnitFun = true)
                    val attendance = mockk<Attendance>(relaxUnitFun = true)
                    every { attendance.user } returns user

                    every { attendanceRepository.findByIdOrNull(1L) } returns attendance

                    val request = UpdateAttendanceStatusRequest(attendanceId = 1L, status = "ABSENT")
                    useCase.execute(listOf(request))

                    verify { attendance.close() }
                    verify { user.removeAttend() }
                    verify { user.absent() }
                }
            }

            context("ATTEND로 변경 시") {
                it("attend + removeAbsent + attend 호출") {
                    val user = mockk<User>(relaxUnitFun = true)
                    val attendance = mockk<Attendance>(relaxUnitFun = true)
                    every { attendance.user } returns user

                    every { attendanceRepository.findByIdOrNull(1L) } returns attendance

                    val request = UpdateAttendanceStatusRequest(attendanceId = 1L, status = "ATTEND")
                    useCase.execute(listOf(request))

                    verify { attendance.attend() }
                    verify { user.removeAbsent() }
                    verify { user.attend() }
                }
            }

            context("출석 정보가 없을 때") {
                it("AttendanceNotFoundException") {
                    every { attendanceRepository.findByIdOrNull(999L) } returns null

                    val request = UpdateAttendanceStatusRequest(attendanceId = 999L, status = "ABSENT")

                    shouldThrow<AttendanceNotFoundException> {
                        useCase.execute(listOf(request))
                    }
                }
            }
        }
    })
