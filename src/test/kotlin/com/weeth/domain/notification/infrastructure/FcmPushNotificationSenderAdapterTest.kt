package com.weeth.domain.notification.infrastructure

import com.google.firebase.messaging.BatchResponse
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.MessagingErrorCode
import com.google.firebase.messaging.SendResponse
import com.weeth.domain.notification.domain.vo.PushNotificationCommand
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.beans.factory.ObjectProvider

class FcmPushNotificationSenderAdapterTest :
    DescribeSpec({
        val firebaseMessaging = mockk<FirebaseMessaging>()
        val firebaseMessagingProvider = mockk<ObjectProvider<FirebaseMessaging>>()
        val adapter = FcmPushNotificationSenderAdapter(firebaseMessagingProvider)

        beforeTest {
            clearMocks(firebaseMessaging, firebaseMessagingProvider)
            every { firebaseMessagingProvider.getObject() } returns firebaseMessaging
        }

        describe("sendMulticast") {
            it("FCM multicast 발송 결과에서 invalid token만 수집한다") {
                val batchResponse = mockk<BatchResponse>()
                val successResponse = mockk<SendResponse>()
                val failedResponse = mockk<SendResponse>()
                val exception = mockk<FirebaseMessagingException>()

                every { firebaseMessaging.sendEachForMulticast(any()) } returns batchResponse
                every { batchResponse.responses } returns listOf(successResponse, failedResponse)
                every { successResponse.isSuccessful } returns true
                every { failedResponse.isSuccessful } returns false
                every { failedResponse.exception } returns exception
                every { exception.messagingErrorCode } returns MessagingErrorCode.UNREGISTERED

                val result = adapter.sendMulticast(createCommand(tokens = listOf("active-token", "invalid-token")))

                result.invalidTokens shouldContainExactly listOf("invalid-token")
                verify(exactly = 1) { firebaseMessaging.sendEachForMulticast(any()) }
            }

            it("token이 비어 있으면 FirebaseMessaging을 호출하지 않는다") {
                val result = adapter.sendMulticast(createCommand(tokens = emptyList()))

                result.invalidTokens shouldBe emptyList()
                verify(exactly = 0) { firebaseMessagingProvider.getObject() }
            }
        }
    }) {
    private companion object {
        fun createCommand(tokens: List<String>): PushNotificationCommand =
            PushNotificationCommand(
                title = "새 공지가 등록되었습니다",
                body = "중간고사 기간 공지",
                tokens = tokens,
                data =
                    mapOf(
                        "type" to "NOTICE_CREATED",
                        "targetPath" to "/clubs/1/boards/10/posts/100",
                    ),
            )
    }
}
