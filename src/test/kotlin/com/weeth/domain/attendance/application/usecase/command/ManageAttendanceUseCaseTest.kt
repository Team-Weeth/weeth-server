package com.weeth.domain.attendance.application.usecase.command

import com.weeth.domain.attendance.application.exception.AlreadyAttendedException
import com.weeth.domain.attendance.application.exception.AttendanceCodeMismatchException
import com.weeth.domain.attendance.application.exception.AttendanceNotFoundException
import com.weeth.domain.attendance.application.exception.QrTokenExpiredException
import com.weeth.domain.attendance.domain.enums.AttendanceStatus
import com.weeth.domain.attendance.domain.port.QrAttendancePort
import com.weeth.domain.attendance.domain.repository.AttendanceRepository
import com.weeth.domain.attendance.fixture.AttendanceTestFixture
import com.weeth.domain.session.application.exception.SessionNotInProgressException
import com.weeth.domain.session.domain.repository.SessionReader
import com.weeth.domain.session.fixture.SessionTestFixture
import com.weeth.domain.user.domain.repository.UserReader
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDateTime

class ManageAttendanceUseCaseTest :
    DescribeSpec({
        val userReader = mockk<UserReader>()
        val sessionReader = mockk<SessionReader>()
        val attendanceRepository = mockk<AttendanceRepository>()
        val qrAttendancePort = mockk<QrAttendancePort>()

        val useCase = ManageAttendanceUseCase(userReader, sessionReader, attendanceRepository, qrAttendancePort)

        beforeTest { clearMocks(userReader, sessionReader, attendanceRepository, qrAttendancePort) }

        describe("checkIn") {
            val userId = 1L
            val code = 123456
            val sessionId = 10L

            context("유효한 코드 + PENDING 상태 + 출석 가능 시간") {
                it("출석 상태를 ATTEND로 변경하고 user.attend()를 호출한다") {
                    val session =
                        SessionTestFixture.createSession(
                            id = sessionId,
                            code = code,
                            start = LocalDateTime.now().minusMinutes(5),
                            end = LocalDateTime.now().plusMinutes(55),
                        )
                    val user = AttendanceTestFixture.createActiveUser("홍길동")
                    val attendance = AttendanceTestFixture.createAttendance(session, user)

                    every { qrAttendancePort.getCode(sessionId) } returns code
                    every { sessionReader.getById(sessionId) } returns session
                    every { userReader.getById(userId) } returns user
                    every { attendanceRepository.findBySessionAndUserWithLock(session, user) } returns attendance

                    useCase.checkIn(userId, sessionId, code)

                    attendance.status shouldBe AttendanceStatus.ATTEND
                }
            }

            context("만료된 QR (Redis miss)") {
                it("QrTokenExpiredException을 던진다") {
                    every { qrAttendancePort.getCode(sessionId) } returns null

                    shouldThrow<QrTokenExpiredException> { useCase.checkIn(userId, sessionId, code) }
                }
            }

            context("코드 불일치") {
                it("AttendanceCodeMismatchException을 던진다") {
                    every { qrAttendancePort.getCode(sessionId) } returns 999999

                    shouldThrow<AttendanceCodeMismatchException> { useCase.checkIn(userId, sessionId, code) }
                }
            }

            context("출석 가능 시간 외 (세션 시작 10분 전 ~ 종료 10분 후 범위 초과)") {
                it("SessionNotInProgressException을 던진다") {
                    val session =
                        SessionTestFixture.createSession(
                            id = sessionId,
                            code = code,
                            start = LocalDateTime.now().minusHours(3),
                            end = LocalDateTime.now().minusHours(1),
                        )

                    every { qrAttendancePort.getCode(sessionId) } returns code
                    every { sessionReader.getById(sessionId) } returns session

                    shouldThrow<SessionNotInProgressException> { useCase.checkIn(userId, sessionId, code) }
                }
            }

            context("이미 ATTEND 상태인 출석") {
                it("AlreadyAttendedException을 던진다") {
                    val session =
                        SessionTestFixture.createSession(
                            id = sessionId,
                            code = code,
                            start = LocalDateTime.now().minusMinutes(5),
                            end = LocalDateTime.now().plusMinutes(55),
                        )
                    val user = AttendanceTestFixture.createActiveUser("홍길동")
                    val attendance = AttendanceTestFixture.createAttendance(session, user).also { it.attend() }

                    every { qrAttendancePort.getCode(sessionId) } returns code
                    every { sessionReader.getById(sessionId) } returns session
                    every { userReader.getById(userId) } returns user
                    every { attendanceRepository.findBySessionAndUserWithLock(session, user) } returns attendance

                    shouldThrow<AlreadyAttendedException> { useCase.checkIn(userId, sessionId, code) }
                }
            }

            context("Attendance 레코드가 없는 경우") {
                it("AttendanceNotFoundException을 던진다") {
                    val session =
                        SessionTestFixture.createSession(
                            id = sessionId,
                            code = code,
                            start = LocalDateTime.now().minusMinutes(5),
                            end = LocalDateTime.now().plusMinutes(55),
                        )
                    val user = AttendanceTestFixture.createActiveUser("홍길동")

                    every { qrAttendancePort.getCode(sessionId) } returns code
                    every { sessionReader.getById(sessionId) } returns session
                    every { userReader.getById(userId) } returns user
                    every { attendanceRepository.findBySessionAndUserWithLock(session, user) } returns null

                    shouldThrow<AttendanceNotFoundException> { useCase.checkIn(userId, sessionId, code) }
                }
            }
        }
    })
