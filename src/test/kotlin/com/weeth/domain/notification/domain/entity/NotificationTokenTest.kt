package com.weeth.domain.notification.domain.entity

import com.weeth.domain.user.fixture.UserTestFixture
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

class NotificationTokenTest :
    StringSpec({

        "reactivate는 토큰 소유자와 등록 시각을 갱신하고 활성화한다" {
            val previousUser = UserTestFixture.createActiveUser1(1L)
            val currentUser = UserTestFixture.createActiveUser2(2L)
            val token =
                NotificationToken.create(
                    user = previousUser,
                    token = "fcm-token",
                    registeredAt = LocalDateTime.of(2026, 9, 21, 10, 0),
                )
            val reRegisteredAt = LocalDateTime.of(2026, 9, 21, 10, 5)

            token.deactivate()
            token.reactivate(user = currentUser, registeredAt = reRegisteredAt)

            token.user shouldBe currentUser
            token.isActive shouldBe true
            token.lastRegisteredAt shouldBe reRegisteredAt
        }
    })
