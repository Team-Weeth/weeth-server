package com.weeth.domain.notification.infrastructure

import com.google.firebase.messaging.BatchResponse
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.MessagingErrorCode
import com.google.firebase.messaging.SendResponse
import com.weeth.domain.notification.domain.enums.NotificationPlatform
import com.weeth.domain.notification.domain.enums.PushTargetStatus
import com.weeth.domain.notification.domain.vo.PushNotificationCommand
import com.weeth.domain.notification.domain.vo.PushTarget
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.springframework.beans.factory.ObjectProvider

class FcmPushNotificationClassificationTest :
    StringSpec({
        val firebase = mockk<FirebaseMessaging>()
        val provider = mockk<ObjectProvider<FirebaseMessaging>>()
        val adapter = FcmPushNotificationSenderAdapter(provider)
        val command =
            PushNotificationCommand(
                "제목",
                "본문",
                listOf("success", "retry", "invalid", "permanent").map {
                    PushTarget(it, NotificationPlatform.WEB)
                },
                emptyMap(),
            )
        beforeTest {
            clearMocks(firebase, provider)
            every { provider.getObject() } returns firebase
        }
        "토큰별 성공·일시 오류·invalid·영구 오류를 구분한다" {
            val batch = mockk<BatchResponse>()
            val codes =
                listOf(
                    null,
                    MessagingErrorCode.UNAVAILABLE,
                    MessagingErrorCode.UNREGISTERED,
                    MessagingErrorCode.INVALID_ARGUMENT,
                )
            every { batch.responses } returns
                codes.map { code ->
                    mockk<SendResponse>().also { response ->
                        every { response.isSuccessful } returns (code == null)
                        if (code != null) {
                            val failure = mockk<FirebaseMessagingException>()
                            every { failure.messagingErrorCode } returns code
                            every { response.exception } returns failure
                        }
                    }
                }
            every { firebase.sendEachForMulticast(any()) } returns batch
            val result = adapter.sendMulticast(command)
            result.outcomes.map { it.status } shouldBe
                listOf(
                    PushTargetStatus.SUCCESS,
                    PushTargetStatus.RETRYABLE,
                    PushTargetStatus.INVALID_TOKEN,
                    PushTargetStatus.PERMANENT_FAILURE,
                )
            result.invalidTokens shouldBe listOf("invalid")
        }
        "외부 호출 예외와 불완전 응답은 확인되지 않은 토큰을 성공으로 처리하지 않는다" {
            every { firebase.sendEachForMulticast(any()) } throws IllegalStateException("temporary error")
            adapter.sendMulticast(command).outcomes.all { it.status == PushTargetStatus.RETRYABLE } shouldBe true
            val batch = mockk<BatchResponse>()
            every { batch.responses } returns emptyList()
            every { firebase.sendEachForMulticast(any()) } returns batch
            adapter.sendMulticast(command).outcomes.size shouldBe 4
            adapter.sendMulticast(command).outcomes.all { it.errorCode == "FCM_INCOMPLETE_RESPONSE" } shouldBe true
        }
    })
