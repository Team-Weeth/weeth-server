package com.weeth.domain.notification.infrastructure

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.weeth.domain.notification.domain.vo.NotificationQueueMessage
import com.weeth.global.config.NotificationDispatchProperties
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest
import software.amazon.awssdk.services.sqs.model.QueueAttributeName
import java.net.URI

class SqsNotificationQueueIntegrationTest :
    StringSpec({
        class ElasticMq : GenericContainer<ElasticMq>(DockerImageName.parse("softwaremill/elasticmq:1.6.12"))
        val container = ElasticMq().withExposedPorts(9324).waitingFor(Wait.forListeningPort())
        lateinit var client: SqsClient
        lateinit var adapter: SqsNotificationQueueAdapter
        lateinit var endpoint: URI
        var sequence = 0
        beforeSpec {
            container.start()
            endpoint = URI.create("http://" + container.host + ":" + container.getMappedPort(9324))
            client =
                SqsClient
                    .builder()
                    .endpointOverride(endpoint)
                    .region(Region.US_EAST_1)
                    .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("local", "local")))
                    .build()
        }
        afterSpec {
            client.close()
            container.stop()
        }
        beforeTest {
            sequence++

            fun url(raw: String) = endpoint.toString() + URI.create(raw).rawPath
            val dlq =
                url(client.createQueue(CreateQueueRequest.builder().queueName("dlq-" + sequence).build()).queueUrl())
            val arn =
                client
                    .getQueueAttributes(
                        GetQueueAttributesRequest
                            .builder()
                            .queueUrl(dlq)
                            .attributeNames(QueueAttributeName.QUEUE_ARN)
                            .build(),
                    ).attributes()[QueueAttributeName.QUEUE_ARN]
            val policy =
                jacksonObjectMapper().writeValueAsString(
                    mapOf(
                        "deadLetterTargetArn" to arn,
                        "maxReceiveCount" to "2",
                    ),
                )
            val source =
                url(
                    client
                        .createQueue(
                            CreateQueueRequest
                                .builder()
                                .queueName("source-" + sequence)
                                .attributes(mapOf(QueueAttributeName.REDRIVE_POLICY to policy))
                                .build(),
                        ).queueUrl(),
                )
            adapter =
                SqsNotificationQueueAdapter(
                    client,
                    jacksonObjectMapper(),
                    NotificationDispatchProperties(queueUrl = source, dlqUrl = dlq),
                )
        }

        "SQS 호환 로컬 큐에서 SDK 배치 송수신과 삭제를 수행한다" {
            val messages = (1L..12L).map { NotificationQueueMessage(it, "event") }
            adapter.publish(messages) shouldBe messages.map { it.deliveryId }.toSet()
            val received = adapter.receive(10, 0, 30) + adapter.receive(10, 0, 30)
            received.map { requireNotNull(it.message).deliveryId }.toSet() shouldBe
                messages.map { it.deliveryId }.toSet()
            received.forEach { adapter.delete(it) }
            adapter.receive(10, 0, 30).isEmpty() shouldBe true
        }

        "visibility 변경 후 재전달된 메시지는 같은 deliveryId를 유지한다" {
            adapter.publish(listOf(NotificationQueueMessage(1, "event")))
            val first = adapter.receive(1, 0, 30).single()
            adapter.receive(1, 0, 30).isEmpty() shouldBe true
            adapter.changeVisibility(first, 0)
            val second = adapter.receive(1, 0, 30).single()
            second.message shouldBe first.message
            second.receiveCount shouldBe 2
            adapter.delete(second)
        }

        "반복 실패한 메시지는 로컬 큐 redrive 정책으로 DLQ에서 추적된다" {
            adapter.publish(listOf(NotificationQueueMessage(1, "event")))
            repeat(4) { adapter.receive(1, 0, 0) }
            val deadLetter = adapter.receive(10, 0, 30, true).single()
            deadLetter.message shouldBe NotificationQueueMessage(1, "event")
            deadLetter.deadLetter shouldBe true
            adapter.delete(deadLetter)
        }
    })
