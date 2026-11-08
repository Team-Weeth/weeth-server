package com.weeth.domain.notification.domain.entity

import com.weeth.domain.notification.domain.enums.NotificationOutboxStatus
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

class NotificationOutboxTest :
    StringSpec({
        "Outbox는 현재 lease 소유자의 결과만 반영하고 실패를 재예약한다" {
            val now = LocalDateTime.of(2026, 11, 8, 12, 0)
            val outbox = NotificationOutbox.create(1L, "event", now)
            outbox.claim("first", now, 60)
            outbox.complete("stale", true, now, 10) shouldBe false
            outbox.status shouldBe NotificationOutboxStatus.PUBLISHING
            outbox.complete("first", false, now, 10) shouldBe true
            outbox.status shouldBe NotificationOutboxStatus.PENDING
            outbox.nextAttemptAt shouldBe now.plusSeconds(10)
            outbox.claim("second", now.plusSeconds(10), 60)
            outbox.complete("second", true, now.plusSeconds(11), 10) shouldBe true
            outbox.status shouldBe NotificationOutboxStatus.PUBLISHED
        }
    })
