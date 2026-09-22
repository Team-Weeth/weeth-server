package com.weeth.domain.notification.application.usecase.command

import com.weeth.domain.notification.application.dto.request.RegisterNotificationTokenRequest
import com.weeth.domain.notification.application.dto.request.RevokeNotificationTokenRequest
import com.weeth.domain.notification.domain.repository.NotificationTokenRepository
import com.weeth.domain.user.domain.repository.UserReader
import com.weeth.domain.user.fixture.UserTestFixture
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import java.time.LocalDateTime

class ManageNotificationTokenUseCaseTest :
    DescribeSpec({
        val userReader = mockk<UserReader>()
        val notificationTokenRepository = mockk<NotificationTokenRepository>()
        val useCase = ManageNotificationTokenUseCase(userReader, notificationTokenRepository)

        beforeTest {
            clearMocks(userReader, notificationTokenRepository)
            every {
                notificationTokenRepository.registerToken(any(), any(), any())
            } returns 1
            every {
                notificationTokenRepository.deactivateByUserIdAndToken(any(), any())
            } returns 1
        }

        describe("register") {
            it("현재 사용자의 웹 FCM 토큰을 등록한다") {
                val user = UserTestFixture.createActiveUser1(10L)
                every { userReader.getById(10L) } returns user

                useCase.register(
                    userId = 10L,
                    request = RegisterNotificationTokenRequest(token = " fcm-token "),
                )

                verify(exactly = 1) { userReader.getById(10L) }
                verify(exactly = 1) {
                    notificationTokenRepository.registerToken(
                        userId = 10L,
                        token = "fcm-token",
                        registeredAt = any<LocalDateTime>(),
                    )
                }
            }
        }

        describe("revoke") {
            it("현재 사용자 소유 토큰만 멱등하게 비활성화한다") {
                useCase.revoke(
                    userId = 10L,
                    request = RevokeNotificationTokenRequest(token = " fcm-token "),
                )

                verify(exactly = 1) {
                    notificationTokenRepository.deactivateByUserIdAndToken(
                        userId = 10L,
                        token = "fcm-token",
                    )
                }
            }
        }

        describe("deactivateInvalidTokens") {
            it("발송 시작 시각 이전 등록 invalid token만 비활성화한다") {
                val sendStartedAt = LocalDateTime.of(2026, 9, 22, 10, 0)
                every {
                    notificationTokenRepository.deactivateInvalidTokens(
                        listOf("invalid-token"),
                        sendStartedAt,
                    )
                } returns 1

                useCase.deactivateInvalidTokens(
                    invalidTokens = listOf("invalid-token"),
                    registeredBeforeOrAt = sendStartedAt,
                )

                verify(exactly = 1) {
                    notificationTokenRepository.deactivateInvalidTokens(
                        invalidTokens = listOf("invalid-token"),
                        registeredBeforeOrAt = sendStartedAt,
                    )
                }
            }

            it("invalid token이 없으면 비활성화 쿼리를 실행하지 않는다") {
                useCase.deactivateInvalidTokens(
                    invalidTokens = emptyList(),
                    registeredBeforeOrAt = LocalDateTime.of(2026, 9, 22, 10, 0),
                )

                verify(exactly = 0) {
                    notificationTokenRepository.deactivateInvalidTokens(any(), any())
                }
            }
        }
    })
