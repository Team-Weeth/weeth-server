package com.weeth.domain.notification.domain.entity

import com.weeth.domain.notification.domain.enums.NotificationType
import com.weeth.domain.user.fixture.UserTestFixture
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

class UserNotificationTest :
    StringSpec({

        "markRead는 읽음 상태와 최초 읽은 시각을 기록하고 재호출 시각으로 덮어쓰지 않는다" {
            val user = UserTestFixture.createActiveUser1(1L)
            val notification =
                UserNotification.create(
                    user = user,
                    type = NotificationType.NOTICE_CREATED,
                    title = "새 공지가 등록되었습니다",
                    body = "중간고사 기간 공지",
                    targetPath = "/clubs/1/boards/10/posts/100",
                    clubId = 1L,
                    boardId = 10L,
                    postId = 100L,
                )
            val firstReadAt = LocalDateTime.of(2026, 9, 21, 10, 0)
            val secondReadAt = LocalDateTime.of(2026, 9, 21, 10, 5)

            notification.isRead shouldBe false
            notification.readAt.shouldBeNull()

            notification.markRead(firstReadAt)
            notification.markRead(secondReadAt)

            notification.isRead shouldBe true
            notification.readAt shouldBe firstReadAt
        }
    })
