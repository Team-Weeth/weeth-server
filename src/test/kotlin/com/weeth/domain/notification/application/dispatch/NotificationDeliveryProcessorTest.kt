package com.weeth.domain.notification.application.dispatch

import com.weeth.domain.notification.application.usecase.command.ManageNotificationDeliveryUseCase
import com.weeth.domain.notification.domain.enums.NotificationDeliveryAction
import com.weeth.domain.notification.domain.enums.NotificationPlatform
import com.weeth.domain.notification.domain.enums.PushTargetStatus
import com.weeth.domain.notification.domain.port.NotificationQueuePort
import com.weeth.domain.notification.domain.port.PushNotificationSenderPort
import com.weeth.domain.notification.domain.vo.NotificationAttemptTarget
import com.weeth.domain.notification.domain.vo.NotificationDeliveryAttempt
import com.weeth.domain.notification.domain.vo.NotificationDeliveryPreparation
import com.weeth.domain.notification.domain.vo.NotificationQueueMessage
import com.weeth.domain.notification.domain.vo.NotificationQueueReceipt
import com.weeth.domain.notification.domain.vo.PushNotificationCommand
import com.weeth.domain.notification.domain.vo.PushNotificationResult
import com.weeth.domain.notification.domain.vo.PushTarget
import com.weeth.domain.notification.domain.vo.PushTargetOutcome
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class NotificationDeliveryProcessorTest :
    DescribeSpec({
        val management = mockk<ManageNotificationDeliveryUseCase>()
        val queue = mockk<NotificationQueuePort>()
        val sender = mockk<PushNotificationSenderPort>()
        val io = Executors.newFixedThreadPool(1)
        val heartbeat = Executors.newScheduledThreadPool(1)
        val target = PushTarget("token", NotificationPlatform.WEB)
        val attempt =
            NotificationDeliveryAttempt(
                1,
                "event",
                "claim",
                2,
                listOf(NotificationAttemptTarget(10, 20, LocalDateTime.of(2026, 11, 8, 12, 0), target)),
                PushNotificationCommand("제목", "본문", listOf(target), emptyMap()),
            )
        val receipt = NotificationQueueReceipt("message", "receipt", NotificationQueueMessage(1, "event"))
        val processor = NotificationDeliveryProcessor(management, queue, sender, io, heartbeat, 1000, 60, 10000)
        beforeTest {
            clearMocks(management, queue, sender)
            every { management.prepare(any()) } returns
                NotificationDeliveryPreparation(NotificationDeliveryAction.READY, attempt)
            every { management.complete(any(), any()) } returns
                NotificationDeliveryPreparation(NotificationDeliveryAction.ACK)
            every { sender.sendMulticast(any()) } returns
                PushNotificationResult(outcomes = listOf(PushTargetOutcome("token", PushTargetStatus.SUCCESS)))
            justRun { queue.delete(any()) }
            justRun { queue.changeVisibility(any(), any()) }
        }
        afterSpec {
            io.shutdownNow()
            heartbeat.shutdownNow()
        }

        it("이미 완료된 작업과 재시도 소진 작업은 FCM을 호출하지 않는다") {
            every { management.prepare(any()) } returns NotificationDeliveryPreparation(NotificationDeliveryAction.ACK)
            processor.process(receipt)
            verify(exactly = 1) { queue.delete(receipt) }
            every { management.prepare(any()) } returns
                NotificationDeliveryPreparation(NotificationDeliveryAction.AWAIT_DLQ)
            processor.process(receipt)
            verify(exactly = 1) { queue.delete(receipt) }
            verify(exactly = 0) { sender.sendMulticast(any()) }
            verify { queue.changeVisibility(receipt, 60) }
        }

        it("잘못된 메시지는 발송하거나 삭제하지 않고 DLQ redrive에 남긴다") {
            processor.process(receipt.copy(message = null))
            verify(exactly = 0) {
                management.prepare(any())
                sender.sendMulticast(any())
                queue.delete(any())
            }
        }

        it("완료 DB 반영이 실패하면 메시지를 삭제하지 않는다") {
            every { management.complete(any(), any()) } throws IllegalStateException("database unavailable")
            processor.process(receipt)
            verify(exactly = 0) { queue.delete(any()) }
        }

        it("외부 발송 시간 예산 초과는 실패 결과로 반영하고 재시도 visibility를 적용한다") {
            every { sender.sendMulticast(any()) } answers
                {
                    CountDownLatch(1).await(5, TimeUnit.SECONDS)
                    PushNotificationResult()
                }
            every { management.complete(any(), any()) } returns
                NotificationDeliveryPreparation(NotificationDeliveryAction.RETRY, delaySeconds = 10)
            NotificationDeliveryProcessor(management, queue, sender, io, heartbeat, 30, 60, 10000).process(receipt)
            verify {
                management.complete(
                    attempt,
                    match {
                        it.outcomes.single().status == PushTargetStatus.RETRYABLE &&
                            it.outcomes.single().errorCode == "FCM_TIMEOUT"
                    },
                )
            }
            verify { queue.changeVisibility(receipt, 10) }
            verify(exactly = 0) { queue.delete(any()) }
        }

        it("발송 대기 중 heartbeat는 DB lease와 SQS visibility를 함께 연장한다") {
            val extended = CountDownLatch(1)
            every { management.extendLease(attempt) } returns true
            every { queue.changeVisibility(receipt, 60) } answers { extended.countDown() }
            every { sender.sendMulticast(any()) } answers {
                check(extended.await(2, TimeUnit.SECONDS))
                PushNotificationResult(outcomes = listOf(PushTargetOutcome("token", PushTargetStatus.SUCCESS)))
            }
            NotificationDeliveryProcessor(management, queue, sender, io, heartbeat, 3000, 60, 10).process(receipt)
            verify { management.extendLease(attempt) }
            verify { queue.changeVisibility(receipt, 60) }
            verify { queue.delete(receipt) }
        }

        it("heartbeat에서 선점 소유권을 잃으면 진행 중 발송 대기를 취소하고 ACK하지 않는다") {
            every { management.extendLease(attempt) } returns false
            every { sender.sendMulticast(any()) } answers {
                CountDownLatch(1).await(5, TimeUnit.SECONDS)
                PushNotificationResult()
            }
            every { management.complete(any(), any()) } returns
                NotificationDeliveryPreparation(NotificationDeliveryAction.RETRY, delaySeconds = 5)
            NotificationDeliveryProcessor(management, queue, sender, io, heartbeat, 3000, 60, 10).process(receipt)
            verify {
                management.complete(attempt, match { it.outcomes.single().status == PushTargetStatus.RETRYABLE })
                queue.changeVisibility(receipt, 5)
            }
            verify(exactly = 0) { queue.delete(any()) }
        }
    })
