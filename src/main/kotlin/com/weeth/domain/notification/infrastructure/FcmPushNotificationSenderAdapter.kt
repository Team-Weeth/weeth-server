package com.weeth.domain.notification.infrastructure

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.MessagingErrorCode
import com.google.firebase.messaging.MulticastMessage
import com.google.firebase.messaging.Notification
import com.weeth.domain.notification.domain.enums.PushTargetStatus
import com.weeth.domain.notification.domain.port.PushNotificationSenderPort
import com.weeth.domain.notification.domain.vo.PushNotificationCommand
import com.weeth.domain.notification.domain.vo.PushNotificationResult
import com.weeth.domain.notification.domain.vo.PushTargetOutcome
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Component

@Component
class FcmPushNotificationSenderAdapter(
    private val firebaseMessagingProvider: ObjectProvider<FirebaseMessaging>,
) : PushNotificationSenderPort {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun sendMulticast(command: PushNotificationCommand): PushNotificationResult {
        if (command.targets.isEmpty()) return PushNotificationResult()
        val firebaseMessaging = firebaseMessagingProvider.getObject()
        val outcomes = mutableListOf<PushTargetOutcome>()
        command.targets.chunked(MAX_MULTICAST_TOKENS).forEach { targets ->
            try {
                val response =
                    firebaseMessaging.sendEachForMulticast(
                        command.copy(targets = targets).toMulticastMessage(),
                    )
                targets.forEachIndexed { index, target ->
                    val item = response.responses.getOrNull(index)
                    outcomes +=
                        when {
                            item == null -> {
                                PushTargetOutcome(
                                    target.token,
                                    PushTargetStatus.RETRYABLE,
                                    "FCM_INCOMPLETE_RESPONSE",
                                )
                            }

                            item.isSuccessful -> {
                                PushTargetOutcome(target.token, PushTargetStatus.SUCCESS)
                            }

                            else -> {
                                outcome(target.token, item.exception)
                            }
                        }
                }
            } catch (exception: Exception) {
                targets.forEach { outcomes += outcome(it.token, exception) }
                log.warn("FCM 배치 발송 실패. tokenCount={}, exceptionType={}", targets.size, exception.javaClass.simpleName)
            }
        }
        val invalidTokens = outcomes.filter { it.status == PushTargetStatus.INVALID_TOKEN }.map { it.token }
        val successCount = outcomes.count { it.status == PushTargetStatus.SUCCESS }
        log.info(
            "FCM multicast 발송 완료. type={}, requestedCount={}, successCount={}, failureCount={}, invalidTokenCount={}",
            command.data["type"] ?: "UNKNOWN",
            command.targets.size,
            successCount,
            outcomes.size - successCount,
            invalidTokens.size,
        )
        return PushNotificationResult(invalidTokens, outcomes)
    }

    private fun outcome(
        token: String,
        exception: Exception?,
    ): PushTargetOutcome {
        val code = (exception as? FirebaseMessagingException)?.messagingErrorCode
        val status =
            when (code) {
                MessagingErrorCode.UNREGISTERED, MessagingErrorCode.SENDER_ID_MISMATCH -> PushTargetStatus.INVALID_TOKEN
                null -> PushTargetStatus.RETRYABLE
                in RETRYABLE_CODES -> PushTargetStatus.RETRYABLE
                else -> PushTargetStatus.PERMANENT_FAILURE
            }
        return PushTargetOutcome(token, status, code?.name ?: "FCM_EXECUTION_FAILED")
    }

    private fun PushNotificationCommand.toMulticastMessage(): MulticastMessage =
        MulticastMessage
            .builder()
            .addAllTokens(targets.map { it.token })
            .setNotification(
                Notification
                    .builder()
                    .setTitle(title)
                    .setBody(body)
                    .build(),
            ).putAllData(data)
            .build()

    private companion object {
        const val MAX_MULTICAST_TOKENS = 500
        val RETRYABLE_CODES =
            setOf(MessagingErrorCode.UNAVAILABLE, MessagingErrorCode.INTERNAL, MessagingErrorCode.QUOTA_EXCEEDED)
    }
}
