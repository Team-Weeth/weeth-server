package com.weeth.domain.notification.presentation

import com.weeth.domain.notification.application.dto.request.RegisterNotificationTokenRequest
import com.weeth.domain.notification.application.dto.request.RevokeNotificationTokenRequest
import com.weeth.domain.notification.application.usecase.command.ManageNotificationTokenUseCase
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.just
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify

class NotificationControllerTest :
    DescribeSpec({
        val manageNotificationTokenUseCase = mockk<ManageNotificationTokenUseCase>()
        val controller = NotificationController(manageNotificationTokenUseCase)

        beforeTest {
            clearMocks(manageNotificationTokenUseCase)
        }

        describe("registerToken") {
            it("토큰 등록 성공 코드를 반환한다") {
                val request = RegisterNotificationTokenRequest(token = "fcm-token")
                justRun { manageNotificationTokenUseCase.register(10L, request) }

                val response = controller.registerToken(request, userId = 10L)

                response.code shouldBe NotificationResponseCode.NOTIFICATION_TOKEN_REGISTERED_SUCCESS.code
                response.message shouldBe NotificationResponseCode.NOTIFICATION_TOKEN_REGISTERED_SUCCESS.message
                response.data shouldBe null
                verify(exactly = 1) { manageNotificationTokenUseCase.register(10L, request) }
            }
        }

        describe("revokeToken") {
            it("토큰 해제 성공 코드를 반환한다") {
                val request = RevokeNotificationTokenRequest(token = "fcm-token")
                justRun { manageNotificationTokenUseCase.revoke(10L, request) }

                val response = controller.revokeToken(request, userId = 10L)

                response.code shouldBe NotificationResponseCode.NOTIFICATION_TOKEN_REVOKED_SUCCESS.code
                response.message shouldBe NotificationResponseCode.NOTIFICATION_TOKEN_REVOKED_SUCCESS.message
                response.data shouldBe null
                verify(exactly = 1) { manageNotificationTokenUseCase.revoke(10L, request) }
            }
        }
    })
