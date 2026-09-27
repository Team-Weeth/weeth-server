package com.weeth.domain.notification.infrastructure

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.MessagingErrorCode
import com.google.firebase.messaging.MulticastMessage
import com.google.firebase.messaging.Notification
import com.weeth.domain.notification.domain.port.PushNotificationSenderPort
import com.weeth.domain.notification.domain.vo.PushNotificationCommand
import com.weeth.domain.notification.domain.vo.PushNotificationResult
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Component

@Component
class FcmPushNotificationSenderAdapter(
    private val firebaseMessagingProvider: ObjectProvider<FirebaseMessaging>,
) : PushNotificationSenderPort {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun sendMulticast(command: PushNotificationCommand): PushNotificationResult {
        if (command.tokens.isEmpty()) {
            return PushNotificationResult()
        }

        val firebaseMessaging = firebaseMessagingProvider.getObject()
        val invalidTokens = mutableListOf<String>()
        var successCount = 0
        var failureCount = 0
        command.tokens.chunked(MAX_MULTICAST_TOKENS).forEach { tokens ->
            try {
                val response =
                    firebaseMessaging.sendEachForMulticast(
                        command.copy(tokens = tokens).toMulticastMessage(),
                    )
                response.responses.forEachIndexed { index, sendResponse ->
                    if (sendResponse.isSuccessful) {
                        successCount++
                    } else {
                        failureCount++
                        sendResponse.exception
                            .takeIf { it.isInvalidTokenError() }
                            ?.let { invalidTokens += tokens[index] }
                    }
                }
            } catch (exception: Exception) {
                failureCount += tokens.size
                log.warn(
                    "FCM multicast 배치 발송 실패. tokenCount={}",
                    tokens.size,
                    exception,
                )
            }
        }

        log.info(
            "FCM multicast 발송 완료. type={}, requestedCount={}, successCount={}, failureCount={}, invalidTokenCount={}",
            command.data["type"] ?: UNKNOWN_NOTIFICATION_TYPE,
            command.tokens.size,
            successCount,
            failureCount,
            invalidTokens.size,
        )

        return PushNotificationResult(invalidTokens = invalidTokens)
    }

    private fun PushNotificationCommand.toMulticastMessage(): MulticastMessage =
        MulticastMessage
            .builder()
            .addAllTokens(tokens)
            .setNotification(
                Notification
                    .builder()
                    .setTitle(title)
                    .setBody(body)
                    .build(),
            ).putAllData(data)
            .build()

    private fun FirebaseMessagingException?.isInvalidTokenError(): Boolean =
        this?.messagingErrorCode in INVALID_TOKEN_ERROR_CODES

    private companion object {
        const val MAX_MULTICAST_TOKENS = 500
        const val UNKNOWN_NOTIFICATION_TYPE = "UNKNOWN"
        val INVALID_TOKEN_ERROR_CODES =
            setOf(
                MessagingErrorCode.UNREGISTERED,
                MessagingErrorCode.SENDER_ID_MISMATCH,
            )
    }
}
