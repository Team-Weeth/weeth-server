package com.weeth.domain.attendance.application.usecase.command

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

class SubscribeAttendanceSseUseCaseTest :
    DescribeSpec({
        val sseSubscribePort = mockk<SseSubscribePort>()
        val clubMemberPolicy = mockk<ClubMemberPolicy>()
        val useCase = SubscribeAttendanceSseUseCase(sseSubscribePort, clubMemberPolicy)

        beforeTest { clearMocks(sseSubscribePort, clubMemberPolicy) }

        describe("execute") {
            val clubId = 1L
            val userId = 100L

            context("활성 멤버인 경우") {
                it("SseSubscribePort를 호출하고 SseEmitter를 반환한다") {
                    val emitter = mockk<SseEmitter>(relaxed = true)

                    every { clubMemberPolicy.getActiveMember(clubId, userId) } returns mockk()
                    every { sseSubscribePort.subscribe(clubId, userId) } returns emitter

                    val result = useCase.execute(clubId, userId)

                    result shouldBe emitter
                    verify(exactly = 1) { clubMemberPolicy.getActiveMember(clubId, userId) }
                    verify(exactly = 1) { sseSubscribePort.subscribe(clubId, userId) }
                }
            }

            context("비활성 멤버이거나 클럽에 속하지 않은 경우") {
                it("예외를 던지고 SSE 구독을 하지 않는다") {
                    every { clubMemberPolicy.getActiveMember(clubId, userId) } throws MemberNotActiveException()

                    shouldThrow<MemberNotActiveException> { useCase.execute(clubId, userId) }

                    verify(exactly = 0) { sseSubscribePort.subscribe(any(), any()) }
                }
            }
        }
    })
