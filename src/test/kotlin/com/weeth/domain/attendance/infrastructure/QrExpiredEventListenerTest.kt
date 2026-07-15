package com.weeth.domain.attendance.infrastructure

import com.weeth.domain.attendance.application.event.AttendanceSseEvent
import com.weeth.domain.attendance.domain.port.QrAttendancePort
import com.weeth.domain.attendance.domain.port.SseBroadcastPort
import com.weeth.domain.session.domain.repository.SessionReader
import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.redis.connection.Message

class QrExpiredEventListenerTest :
    DescribeSpec({
        val sessionReader = mockk<SessionReader>()
        val qrAttendancePort = mockk<QrAttendancePort>()
        val sseBroadcastPort = mockk<SseBroadcastPort>(relaxed = true)
        val listener = QrExpiredEventListener(sessionReader, qrAttendancePort, sseBroadcastPort)

        beforeTest { clearMocks(sessionReader, qrAttendancePort, sseBroadcastPort) }

        fun message(key: String): Message = mockk { every { body } returns key.toByteArray() }

        describe("onMessage") {
            context("qr:{sessionId} 키가 만료된 경우") {
                it("현재 활성 QR과 일치하면 해당 클럽에 qr-close를 broadcast한다") {
                    every { sessionReader.findClubIdById(42L) } returns 7L
                    every { qrAttendancePort.clearActiveSessionIfMatches(7L, 42L) } returns true

                    listener.onMessage(message("qr:42"), null)

                    verify { sseBroadcastPort.broadcast(7L, AttendanceSseEvent.QR_CLOSE, null) }
                }

                it("현재 활성 QR과 일치하지 않으면 broadcast하지 않는다") {
                    every { sessionReader.findClubIdById(42L) } returns 7L
                    every { qrAttendancePort.clearActiveSessionIfMatches(7L, 42L) } returns false

                    listener.onMessage(message("qr:42"), null)

                    verify(exactly = 0) { sseBroadcastPort.broadcast(any(), any(), any()) }
                }
            }

            context("qr: 접두사가 아닌 키가 만료된 경우") {
                it("broadcast하지 않는다") {
                    listener.onMessage(message("other:42"), null)

                    verify(exactly = 0) { sseBroadcastPort.broadcast(any(), any(), any()) }
                }
            }

            context("sessionId가 숫자가 아닌 경우") {
                it("broadcast하지 않는다") {
                    listener.onMessage(message("qr:invalid"), null)

                    verify(exactly = 0) { sseBroadcastPort.broadcast(any(), any(), any()) }
                }
            }

            context("세션이 존재하지 않는 경우") {
                it("broadcast하지 않는다") {
                    every { sessionReader.findClubIdById(99L) } returns null

                    listener.onMessage(message("qr:99"), null)

                    verify(exactly = 0) { sseBroadcastPort.broadcast(any(), any(), any()) }
                }
            }

            context("broadcast 중 예외가 발생하는 경우") {
                it("예외가 전파되지 않는다") {
                    every { sessionReader.findClubIdById(42L) } returns 7L
                    every { qrAttendancePort.clearActiveSessionIfMatches(7L, 42L) } returns true
                    every { sseBroadcastPort.broadcast(any(), any(), any()) } throws RuntimeException("network error")

                    shouldNotThrow<Exception> { listener.onMessage(message("qr:42"), null) }
                }
            }
        }
    })
