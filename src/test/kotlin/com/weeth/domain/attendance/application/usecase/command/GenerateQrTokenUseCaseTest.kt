package com.weeth.domain.attendance.application.usecase.command

import com.weeth.domain.attendance.application.dto.response.QrTokenResponse
import com.weeth.domain.attendance.application.mapper.AttendanceMapper
import com.weeth.domain.attendance.domain.port.QrAttendancePort
import com.weeth.domain.session.application.exception.SessionNotFoundException
import com.weeth.domain.session.domain.repository.SessionReader
import com.weeth.domain.session.fixture.SessionTestFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import io.mockk.verify
import java.time.LocalDateTime

class GenerateQrTokenUseCaseTest : DescribeSpec({
    val sessionReader = mockk<SessionReader>()
    val qrAttendancePort = mockk<QrAttendancePort>()
    val attendanceMapper = mockk<AttendanceMapper>()

    val useCase = GenerateQrTokenUseCase(sessionReader, qrAttendancePort, attendanceMapper)

    beforeTest { clearMocks(sessionReader, qrAttendancePort, attendanceMapper) }

    describe("execute") {
        val sessionId = 1L
        val code = 123456

        context("유효한 sessionId") {
            it("Redis에 코드를 저장하고 QrTokenResponse를 반환한다") {
                val session = SessionTestFixture.createSession(id = sessionId, code = code)
                val expectedResponse = QrTokenResponse(code = code, expiredAt = LocalDateTime.now().plusSeconds(600))

                every { sessionReader.getById(sessionId) } returns session
                every { qrAttendancePort.store(code, sessionId) } just Runs
                every { attendanceMapper.toQrTokenResponse(eq(session), any()) } returns expectedResponse

                val result = useCase.execute(sessionId)

                result shouldBe expectedResponse
                verify(exactly = 1) { qrAttendancePort.store(code, sessionId) }
            }
        }

        context("존재하지 않는 sessionId") {
            it("SessionNotFoundException을 던진다") {
                every { sessionReader.getById(sessionId) } throws SessionNotFoundException()

                shouldThrow<SessionNotFoundException> { useCase.execute(sessionId) }

                verify(exactly = 0) { qrAttendancePort.store(any(), any()) }
            }
        }
    }
})
