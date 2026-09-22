package com.weeth.domain.notification.infrastructure

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.MessagingErrorCode
import com.google.firebase.messaging.MulticastMessage
import com.google.firebase.messaging.Notification
import com.weeth.domain.notification.domain.port.PushNotificationSenderPort
import com.weeth.domain.notification.domain.vo.PushNotificationCommand
import com.weeth.domain.notification.domain.vo.PushNotificationResult
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Component

@Component
class FcmPushNotificationSenderAdapter(
    private val firebaseMessagingProvider: ObjectProvider<FirebaseMessaging>,
) : PushNotificationSenderPort {
    override fun sendMulticast(command: PushNotificationCommand): PushNotificationResult {
        if (command.tokens.isEmpty()) {
            return PushNotificationResult()
        }

        val response =
            firebaseMessagingProvider
                .getObject()
                .sendEachForMulticast(command.toMulticastMessage())
        val invalidTokens =
            response.responses.mapIndexedNotNull { index, sendResponse ->
                if (sendResponse.isSuccessful) {
                    null
                } else {
                    sendResponse.exception
                        .takeIf { it.isInvalidTokenError() }
                        ?.let { command.tokens[index] }
                }
            }

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
        this?.messagingErrorCode in
            setOf(
                MessagingErrorCode.UNREGISTERED,
                MessagingErrorCode.INVALID_ARGUMENT,
            )
}
