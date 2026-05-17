package com.weeth.domain.attendance.application.usecase.command

import com.weeth.domain.attendance.application.dto.response.QrTokenResponse
import com.weeth.domain.attendance.application.event.AttendanceOpenEvent
import com.weeth.domain.attendance.application.event.AttendanceSseEvent
import com.weeth.domain.attendance.application.mapper.AttendanceMapper
import com.weeth.domain.attendance.domain.port.QrAttendancePort
import com.weeth.domain.attendance.domain.port.SseBroadcastPort
import com.weeth.domain.club.domain.service.ClubPermissionPolicy
import com.weeth.domain.club.fixture.ClubTestFixture
import com.weeth.domain.session.application.exception.SessionNotFoundException
import com.weeth.domain.session.domain.repository.SessionReader
import com.weeth.domain.session.fixture.SessionTestFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.springframework.transaction.PlatformTransactionManager
import java.time.LocalDateTime

class GenerateQrTokenUseCaseTest :
    DescribeSpec({
        val sessionReader = mockk<SessionReader>()
        val qrAttendancePort = mockk<QrAttendancePort>()
        val attendanceMapper = mockk<AttendanceMapper>()
        val clubPermissionPolicy = mockk<ClubPermissionPolicy>(relaxed = true)
        val ssePort = mockk<SseBroadcastPort>(relaxed = true)
        val transactionManager = mockk<PlatformTransactionManager>(relaxed = true)

        val useCase =
            GenerateQrTokenUseCase(
                sessionReader,
                qrAttendancePort,
                attendanceMapper,
                clubPermissionPolicy,
                ssePort,
                transactionManager,
            )

        beforeTest {
            clearMocks(sessionReader, qrAttendancePort, attendanceMapper, clubPermissionPolicy, ssePort)
        }

        describe("execute") {
            val sessionId = 1L
            val code = 123456
            val clubId = 10L

            context("유효한 sessionId") {
                it("Redis에 코드를 저장하고 QrTokenResponse를 반환한다") {
                    val session =
                        SessionTestFixture.createSession(
                            id = sessionId,
                            code = code,
                            club = ClubTestFixture.createClub(id = clubId),
                        )
                    val expectedResponse =
                        QrTokenResponse(
                            sessionId = sessionId,
                            code = code,
                            expiredAt = LocalDateTime.now().plusSeconds(QrAttendancePort.TTL_SECONDS),
                        )

                    every { sessionReader.getById(sessionId) } returns session
                    every { qrAttendancePort.store(clubId, sessionId, code) } just Runs
                    every { attendanceMapper.toQrTokenResponse(eq(session), any()) } returns expectedResponse

                    val result = useCase.execute(sessionId, clubId, 20L)

                    result shouldBe expectedResponse
                    verify(exactly = 1) { clubPermissionPolicy.requireAdmin(clubId, 20L) }
                    verify(exactly = 1) { qrAttendancePort.store(clubId, sessionId, code) }
                    verify(exactly = 1) {
                        ssePort.broadcast(clubId, AttendanceSseEvent.QR_OPEN, any<AttendanceOpenEvent>())
                    }
                }
            }

            context("존재하지 않는 sessionId") {
                it("SessionNotFoundException을 던진다") {
                    every { sessionReader.getById(sessionId) } throws SessionNotFoundException()

                    shouldThrow<SessionNotFoundException> { useCase.execute(sessionId, clubId, 20L) }

                    verify(exactly = 0) { qrAttendancePort.store(any(), any(), any()) }
                    verify(exactly = 0) { ssePort.broadcast(any(), any(), any()) }
                }
            }

            context("다른 클럽의 sessionId") {
                it("SessionNotFoundException을 던지고 QR을 저장하지 않는다") {
                    val session =
                        SessionTestFixture.createSession(
                            id = sessionId,
                            code = code,
                            club = ClubTestFixture.createClub(id = 999L),
                        )
                    every { sessionReader.getById(sessionId) } returns session

                    shouldThrow<SessionNotFoundException> { useCase.execute(sessionId, clubId, 20L) }

                    verify(exactly = 1) { clubPermissionPolicy.requireAdmin(clubId, 20L) }
                    verify(exactly = 0) { qrAttendancePort.store(any(), any(), any()) }
                    verify(exactly = 0) { ssePort.broadcast(any(), any(), any()) }
                }
            }
        }
    })
