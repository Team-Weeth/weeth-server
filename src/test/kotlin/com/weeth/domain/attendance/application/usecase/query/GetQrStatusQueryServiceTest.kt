package com.weeth.domain.attendance.application.usecase.query

import com.weeth.domain.attendance.application.mapper.AttendanceMapper
import com.weeth.domain.attendance.domain.port.QrAttendancePort
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime

class GetQrStatusQueryServiceTest :
    DescribeSpec({
        val clubMemberPolicy = mockk<ClubMemberPolicy>()
        val qrAttendancePort = mockk<QrAttendancePort>()
        val attendanceMapper = AttendanceMapper()
        val queryService = GetQrStatusQueryService(clubMemberPolicy, qrAttendancePort, attendanceMapper)

        beforeTest {
            clearMocks(clubMemberPolicy, qrAttendancePort)
        }

        describe("findQrStatus") {
            val clubId = 1L
            val userId = 100L

            beforeTest {
                every { clubMemberPolicy.getActiveMember(clubId, userId) } returns mockk()
            }

            context("활성 QR 세션이 없는 경우") {
                it("비활성 상태를 반환한다") {
                    every { qrAttendancePort.getActiveSessionId(clubId) } returns null

                    val result = queryService.findQrStatus(clubId, userId)

                    result.isActive shouldBe false
                    result.currentSessionId shouldBe null
                    result.expiresAt shouldBe null
                }
            }

            context("활성 QR 세션의 TTL이 남아 있는 경우") {
                it("활성 상태와 만료 시각을 반환한다") {
                    val sessionId = 10L
                    val expiresAt = LocalDateTime.now().plusMinutes(5)
                    every { qrAttendancePort.getActiveSessionId(clubId) } returns sessionId
                    every { qrAttendancePort.getExpiredAt(sessionId) } returns expiresAt

                    val result = queryService.findQrStatus(clubId, userId)

                    result.isActive shouldBe true
                    result.currentSessionId shouldBe sessionId
                    result.expiresAt shouldBe expiresAt
                }
            }

            context("활성 QR 세션은 있지만 QR TTL이 만료된 경우") {
                it("비활성 상태를 반환한다") {
                    val sessionId = 10L
                    every { qrAttendancePort.getActiveSessionId(clubId) } returns sessionId
                    every { qrAttendancePort.getExpiredAt(sessionId) } returns null

                    val result = queryService.findQrStatus(clubId, userId)

                    result.isActive shouldBe false
                    result.currentSessionId shouldBe null
                    result.expiresAt shouldBe null
                }
            }

            it("활성 멤버 여부를 확인한다") {
                every { qrAttendancePort.getActiveSessionId(clubId) } returns null

                queryService.findQrStatus(clubId, userId)

                verify(exactly = 1) { clubMemberPolicy.getActiveMember(clubId, userId) }
            }
        }
    })
