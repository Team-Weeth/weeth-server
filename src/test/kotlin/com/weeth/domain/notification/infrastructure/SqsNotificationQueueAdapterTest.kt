package com.weeth.domain.notification.infrastructure

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.weeth.domain.notification.domain.vo.NotificationQueueMessage
import com.weeth.global.config.NotificationDispatchProperties
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.Message
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResultEntry

class SqsNotificationQueueAdapterTest :
    StringSpec({
        val client = mockk<SqsClient>()
        val mapper = jacksonObjectMapper()
        val adapter =
            SqsNotificationQueueAdapter(
                client,
                mapper,
                NotificationDispatchProperties(queueUrl = "http://localhost/queue", dlqUrl = "http://localhost/dlq"),
            )
        beforeTest { clearMocks(client) }

        "10개씩 배치 전송하며 HTTP 성공이어도 성공 항목 ID만 반환한다" {
            every { client.sendMessageBatch(any<SendMessageBatchRequest>()) } answers {
                val request = firstArg<SendMessageBatchRequest>()
                SendMessageBatchResponse
                    .builder()
                    .successful(
                        request.entries().dropLast(1).map {
                            SendMessageBatchResultEntry
                                .builder()
                                .id(it.id())
                                .messageId(it.id())
                                .build()
                        },
                    ).build()
            }
            val result = adapter.publish((1L..12L).map { NotificationQueueMessage(it, "event") })
            result shouldBe ((1L..9L).toSet() + 11L)
            verify(exactly = 2) { client.sendMessageBatch(any<SendMessageBatchRequest>()) }
            verify {
                client.sendMessageBatch(
                    match<SendMessageBatchRequest> {
                        it.entries().all { entry ->
                            mapper
                                .readTree(entry.messageBody())
                                .fieldNames()
                                .asSequence()
                                .toSet() ==
                                setOf("schemaVersion", "deliveryId", "eventId", "generation")
                        }
                    },
                )
            }
        }

        "지원하지 않는 schema와 손상된 JSON을 개별 invalid receipt로 남긴다" {
            every { client.receiveMessage(any<ReceiveMessageRequest>()) } returns
                ReceiveMessageResponse
                    .builder()
                    .messages(
                        Message
                            .builder()
                            .messageId(
                                "one",
                            ).receiptHandle("a")
                            .body("""{"schemaVersion":1,"deliveryId":1,"eventId":"event"}""")
                            .build(),
                        Message
                            .builder()
                            .messageId(
                                "two",
                            ).receiptHandle("b")
                            .body("""{"schemaVersion":2,"deliveryId":2,"eventId":"event"}""")
                            .build(),
                        Message
                            .builder()
                            .messageId("three")
                            .receiptHandle("c")
                            .body("broken")
                            .build(),
                        *listOf("4294967297", "18446744073709551617")
                            .mapIndexed { index, version ->
                                Message
                                    .builder()
                                    .messageId("overflow-$index")
                                    .receiptHandle("overflow-$index")
                                    .body("""{"schemaVersion":$version,"deliveryId":1,"eventId":"event"}""")
                                    .build()
                            }.toTypedArray(),
                    ).build()
            val receipts = adapter.receive(5, 20, 60)
            receipts[0].message shouldBe NotificationQueueMessage(1, "event", schemaVersion = 1)
            receipts[1].message shouldBe null
            receipts[2].message shouldBe null
            receipts[3].message shouldBe null
            receipts[4].message shouldBe null
            verify {
                client.receiveMessage(
                    match<ReceiveMessageRequest> {
                        it.waitTimeSeconds() == 20 &&
                            it.visibilityTimeout() == 60 &&
                            it.maxNumberOfMessages() == 5
                    },
                )
            }
        }

        "새 schema는 발송 세대를 보존하고 잘못된 세대 값은 거부한다" {
            val generations = listOf("7", "-1", "1.5", "\"7\"", "18446744073709551616", "null")
            every { client.receiveMessage(any<ReceiveMessageRequest>()) } returns
                ReceiveMessageResponse
                    .builder()
                    .messages(
                        generations.mapIndexed { index, generation ->
                            Message
                                .builder()
                                .messageId("g$index")
                                .receiptHandle("r$index")
                                .body(
                                    """{"schemaVersion":2,"deliveryId":1,"eventId":"event","generation":$generation}""",
                                ).build()
                        },
                    ).build()
            val receipts = adapter.receive(6, 20, 60)
            receipts[0].message shouldBe NotificationQueueMessage(1, "event", generation = 7)
            receipts.drop(1).all { it.message == null } shouldBe true
        }

        "후속 배치가 예외로 실패해도 앞서 전송된 배치의 성공은 보존한다" {
            every { client.sendMessageBatch(any<SendMessageBatchRequest>()) } answers {
                val request = firstArg<SendMessageBatchRequest>()
                if (request.entries().first().id() == "11") error("second batch unavailable")
                SendMessageBatchResponse
                    .builder()
                    .successful(
                        request.entries().map {
                            SendMessageBatchResultEntry
                                .builder()
                                .id(it.id())
                                .messageId(it.id())
                                .build()
                        },
                    ).build()
            }
            adapter.publish((1L..12L).map { NotificationQueueMessage(it, "event") }) shouldBe (1L..10L).toSet()
        }
    })
