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

            it("token이 500개를 초과하면 500개씩 나누어 발송한다") {
                val batchResponse = mockk<BatchResponse>()
                every { batchResponse.responses } returns emptyList()
                every { firebaseMessaging.sendEachForMulticast(any()) } returns batchResponse

                adapter.sendMulticast(createCommand(tokens = (1..501).map { "token-$it" }))

                verify(exactly = 2) { firebaseMessaging.sendEachForMulticast(any()) }
            }

            it("유효한 payload에서 INVALID_ARGUMENT 응답 token을 invalid token으로 수집한다") {
                val batchResponse = mockk<BatchResponse>()
                val failedResponse = mockk<SendResponse>()
                val exception = mockk<FirebaseMessagingException>()

                every { firebaseMessaging.sendEachForMulticast(any()) } returns batchResponse
                every { batchResponse.responses } returns listOf(failedResponse)
                every { failedResponse.isSuccessful } returns false
                every { failedResponse.exception } returns exception
                every { exception.messagingErrorCode } returns MessagingErrorCode.INVALID_ARGUMENT

                val result = adapter.sendMulticast(createCommand(tokens = listOf("active-token")))

                result.invalidTokens shouldContainExactly listOf("active-token")
            }

            it("중간 배치 발송이 실패해도 다음 배치를 계속 발송하고 이전 invalid token을 반환한다") {
                val firstBatchResponse = mockk<BatchResponse>()
                val lastBatchResponse = mockk<BatchResponse>()
                val invalidResponse = mockk<SendResponse>()
                val exception = mockk<FirebaseMessagingException>()
                var invocationCount = 0

                every { firstBatchResponse.responses } returns listOf(invalidResponse)
                every { lastBatchResponse.responses } returns emptyList()
                every { invalidResponse.isSuccessful } returns false
                every { invalidResponse.exception } returns exception
                every { exception.messagingErrorCode } returns MessagingErrorCode.UNREGISTERED
                every { firebaseMessaging.sendEachForMulticast(any()) } answers {
                    invocationCount++
                    when (invocationCount) {
                        1 -> firstBatchResponse
                        2 -> throw RuntimeException("temporary FCM failure")
                        else -> lastBatchResponse
                    }
                }

                val result = adapter.sendMulticast(createCommand(tokens = (1..1001).map { "token-$it" }))

                verify(exactly = 3) { firebaseMessaging.sendEachForMulticast(any()) }
                result.invalidTokens shouldContainExactly listOf("token-1")
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
