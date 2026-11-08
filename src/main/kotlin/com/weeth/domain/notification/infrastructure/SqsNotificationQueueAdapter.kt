package com.weeth.domain.notification.infrastructure

import com.fasterxml.jackson.databind.ObjectMapper
import com.weeth.domain.notification.domain.port.NotificationQueuePort
import com.weeth.domain.notification.domain.vo.NotificationQueueMessage
import com.weeth.domain.notification.domain.vo.NotificationQueueReceipt
import com.weeth.global.config.NotificationDispatchProperties
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityRequest
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry

@Component
@ConditionalOnProperty(prefix = "notification.dispatch", name = ["enabled"], havingValue = "true")
class SqsNotificationQueueAdapter(
    private val client: SqsClient,
    private val objectMapper: ObjectMapper,
    private val properties: NotificationDispatchProperties,
) : NotificationQueuePort {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun publish(messages: List<NotificationQueueMessage>): Set<Long> =
        buildSet {
            messages.chunked(10).forEach { batch ->
                val requestedIds = batch.map { it.deliveryId }.toSet()
                val response =
                    try {
                        client.sendMessageBatch(
                            SendMessageBatchRequest
                                .builder()
                                .queueUrl(properties.queueUrl)
                                .entries(
                                    batch.map {
                                        SendMessageBatchRequestEntry
                                            .builder()
                                            .id(it.deliveryId.toString())
                                            .messageBody(objectMapper.writeValueAsString(it))
                                            .build()
                                    },
                                ).build(),
                        )
                    } catch (_: Exception) {
                        log.warn("알림 SQS 배치 전송 실패. batchSize={}", batch.size)
                        null
                    }
                response
                    ?.successful()
                    ?.mapNotNull { it.id().toLongOrNull() }
                    ?.filter { it in requestedIds }
                    ?.forEach { add(it) }
            }
        }

    override fun receive(
        maxMessages: Int,
        waitSeconds: Int,
        visibilitySeconds: Int,
        deadLetter: Boolean,
    ): List<NotificationQueueReceipt> {
        val response =
            client.receiveMessage(
                ReceiveMessageRequest
                    .builder()
                    .queueUrl(url(deadLetter))
                    .maxNumberOfMessages(maxMessages.coerceIn(1, 10))
                    .waitTimeSeconds(waitSeconds.coerceIn(0, 20))
                    .visibilityTimeout(visibilitySeconds.coerceIn(0, 43200))
                    .messageSystemAttributeNames(MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT)
                    .build(),
            )
        return response.messages().map { message ->
            NotificationQueueReceipt(
                message.messageId(),
                message.receiptHandle(),
                decode(message.body()),
                message.attributes()[MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT]?.toIntOrNull() ?: 1,
                deadLetter,
            )
        }
    }

    override fun delete(receipt: NotificationQueueReceipt) {
        client.deleteMessage(
            DeleteMessageRequest
                .builder()
                .queueUrl(
                    url(receipt.deadLetter),
                ).receiptHandle(receipt.receiptHandle)
                .build(),
        )
    }

    override fun changeVisibility(
        receipt: NotificationQueueReceipt,
        seconds: Int,
    ) {
        client.changeMessageVisibility(
            ChangeMessageVisibilityRequest
                .builder()
                .queueUrl(url(receipt.deadLetter))
                .receiptHandle(receipt.receiptHandle)
                .visibilityTimeout(seconds.coerceIn(0, 43200))
                .build(),
        )
    }

    private fun decode(body: String): NotificationQueueMessage? =
        try {
            val tree = objectMapper.readTree(body)
            val version = tree?.get("schemaVersion")
            val id = tree?.get("deliveryId")
            val eventId = tree?.get("eventId")
            if (version?.isIntegralNumber != true || !version.canConvertToInt() || version.asInt(-1) != 1 ||
                id?.isIntegralNumber != true ||
                !id.canConvertToLong() || id.asLong() <= 0 ||
                eventId?.isTextual != true || eventId.asText().isBlank() ||
                eventId.asText().length > 150
            ) {
                null
            } else {
                NotificationQueueMessage(id.asLong(), eventId.asText())
            }
        } catch (_: Exception) {
            null
        }

    private fun url(deadLetter: Boolean): String = if (deadLetter) properties.dlqUrl else properties.queueUrl
}
