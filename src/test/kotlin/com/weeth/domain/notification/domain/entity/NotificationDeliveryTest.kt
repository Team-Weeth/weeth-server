package com.weeth.domain.notification.domain.entity

import com.weeth.domain.notification.domain.enums.NotificationDeliveryStatus
import com.weeth.domain.notification.domain.enums.NotificationTargetStatus
import com.weeth.domain.notification.domain.enums.NotificationType
import com.weeth.domain.notification.domain.enums.PushTargetStatus
import com.weeth.domain.notification.domain.service.NotificationRetryPolicy
import com.weeth.domain.notification.domain.vo.PushTargetOutcome
import com.weeth.domain.notification.domain.vo.RenderedNotification
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

class NotificationDeliveryTest :
    StringSpec({
        val now = LocalDateTime.of(2026, 11, 8, 12, 0)

        fun delivery() =
            NotificationDelivery.create(
                "NOTICE_CREATED:1:10",
                2L,
                1L,
                NotificationType.NOTICE_CREATED,
                RenderedNotification("제목", "본문", 1),
                mapOf("type" to "NOTICE_CREATED"),
                now,
            )

        "성공 토큰은 유지하고 남은 일시 실패만 재시도하며 한도 소진을 추적한다" {
            val delivery = delivery()
            val success = NotificationDeliveryTarget.create(1L, 10L, now)
            val pending = NotificationDeliveryTarget.create(1L, 20L, now)
            delivery.claim("first", now, 60)
            delivery.beginAttempt()
            success.record(PushTargetOutcome("a", PushTargetStatus.SUCCESS), now)
            pending.record(PushTargetOutcome("b", PushTargetStatus.RETRYABLE, "UNAVAILABLE"), now)
            delivery.finishAttempt(
                "first",
                now,
                listOf(success, pending),
                NotificationRetryPolicy(2, listOf(10)),
            ) shouldBe
                true
            delivery.status shouldBe NotificationDeliveryStatus.RETRY_WAIT
            success.status shouldBe NotificationTargetStatus.SUCCEEDED
            pending.status shouldBe NotificationTargetStatus.PENDING
            delivery.claim("second", now.plusSeconds(20), 60)
            delivery.beginAttempt()
            delivery.finishAttempt(
                "second",
                now.plusSeconds(20),
                listOf(success, pending),
                NotificationRetryPolicy(2, listOf(10)),
            ) shouldBe
                true
            delivery.status shouldBe NotificationDeliveryStatus.FAILED
            delivery.awaitingDeadLetter shouldBe true
            delivery.attemptCount shouldBe 2
            delivery.requeue(now.plusSeconds(30))
            delivery.status shouldBe NotificationDeliveryStatus.PENDING
            delivery.attemptCount shouldBe 0
            success.status shouldBe NotificationTargetStatus.SUCCEEDED
        }

        "영구 실패와 수신 거부는 자동 재시도하지 않는다" {
            val delivery = delivery()
            val target = NotificationDeliveryTarget.create(1L, 10L, now)
            delivery.claim("owner", now, 60)
            delivery.beginAttempt()
            target.record(PushTargetOutcome("a", PushTargetStatus.PERMANENT_FAILURE, "INVALID_ARGUMENT"), now)
            delivery.finishAttempt("owner", now, listOf(target), NotificationRetryPolicy())
            delivery.status shouldBe NotificationDeliveryStatus.FAILED
            delivery.awaitingDeadLetter shouldBe false
            val skipped = delivery()
            skipped.skip(now, "PREFERENCE_DISABLED")
            skipped.status shouldBe NotificationDeliveryStatus.SKIPPED
        }
    })
