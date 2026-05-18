package com.weeth.domain.attendance.application.usecase.command

import com.weeth.domain.attendance.application.event.AttendanceOpenEvent
import com.weeth.domain.attendance.application.event.AttendanceSseEvent
import com.weeth.domain.attendance.domain.port.QrAttendancePort
import com.weeth.domain.attendance.domain.port.SseBroadcastPort
import com.weeth.domain.attendance.domain.port.SseSubscribePort
import com.weeth.domain.club.application.exception.MemberNotActiveException
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.time.LocalDateTime

class SubscribeAttendanceSseUseCaseTest :
    DescribeSpec({
        val sseSubscribePort = mockk<SseSubscribePort>()
        val sseBroadcastPort = mockk<SseBroadcastPort>(relaxed = true)
        val clubMemberPolicy = mockk<ClubMemberPolicy>()
        val qrAttendancePort = mockk<QrAttendancePort>()
        val useCase =
            SubscribeAttendanceSseUseCase(
                sseSubscribePort,
                sseBroadcastPort,
                clubMemberPolicy,
                qrAttendancePort,
            )

        beforeTest { clearMocks(sseSubscribePort, sseBroadcastPort, clubMemberPolicy, qrAttendancePort) }

        describe("execute") {
            val clubId = 1L
            val userId = 100L
            val emitter = mockk<SseEmitter>(relaxed = true)

            beforeTest {
                every { clubMemberPolicy.getActiveMember(clubId, userId) } returns mockk()
                every { sseSubscribePort.subscribe(clubId, userId) } returns emitter
            }

            context("활성 QR이 없는 경우") {
                it("qr-none 이벤트를 전송하고 emitter를 반환한다") {
                    every { qrAttendancePort.getActiveSessionId(clubId) } returns null

                    val result = useCase.execute(clubId, userId)

                    result shouldBe emitter
                    verify(
                        exactly = 1,
                    ) { sseBroadcastPort.sendToUser(clubId, userId, AttendanceSseEvent.QR_NONE, null) }
                    verify(
                        exactly = 0,
                    ) { sseBroadcastPort.sendToUser(clubId, userId, AttendanceSseEvent.QR_OPEN, any()) }
                }
            }

            context("활성 QR 세션은 있지만 QR이 만료된 경우") {
                it("qr-none 이벤트를 전송한다") {
                    every { qrAttendancePort.getActiveSessionId(clubId) } returns 10L
                    every { qrAttendancePort.getExpiredAt(10L) } returns null

                    useCase.execute(clubId, userId)

                    verify(
                        exactly = 1,
                    ) { sseBroadcastPort.sendToUser(clubId, userId, AttendanceSseEvent.QR_NONE, null) }
                }
            }

            context("활성 QR이 있는 경우") {
                it("qr-open 이벤트를 전송하고 emitter를 반환한다") {
                    val expiredAt = LocalDateTime.now().plusMinutes(5)
                    every { qrAttendancePort.getActiveSessionId(clubId) } returns 10L
                    every { qrAttendancePort.getExpiredAt(10L) } returns expiredAt

                    val result = useCase.execute(clubId, userId)

                    result shouldBe emitter
                    verify(exactly = 1) {
                        sseBroadcastPort.sendToUser(
                            clubId,
                            userId,
                            AttendanceSseEvent.QR_OPEN,
                            AttendanceOpenEvent(expiredAt),
                        )
                    }
                    verify(
                        exactly = 0,
                    ) { sseBroadcastPort.sendToUser(clubId, userId, AttendanceSseEvent.QR_NONE, any()) }
                }
            }

            context("비활성 멤버이거나 클럽에 속하지 않은 경우") {
                it("예외를 던지고 SSE 구독을 하지 않는다") {
                    every { clubMemberPolicy.getActiveMember(clubId, userId) } throws MemberNotActiveException()

                    shouldThrow<MemberNotActiveException> { useCase.execute(clubId, userId) }

                    verify(exactly = 0) { sseSubscribePort.subscribe(any(), any()) }
                    verify(exactly = 0) { sseBroadcastPort.sendToUser(any(), any(), any(), any()) }
                }
            }
        }
    })
