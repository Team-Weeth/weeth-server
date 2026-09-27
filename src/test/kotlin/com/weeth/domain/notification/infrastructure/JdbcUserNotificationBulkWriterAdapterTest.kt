package com.weeth.domain.notification.infrastructure

import com.weeth.domain.notification.domain.entity.UserNotification
import com.weeth.domain.notification.domain.enums.NotificationType
import com.weeth.domain.user.fixture.UserTestFixture
import io.kotest.core.spec.style.StringSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.PreparedStatementSetter

class JdbcUserNotificationBulkWriterAdapterTest :
    StringSpec({
        val jdbcTemplate = mockk<JdbcTemplate>()
        val adapter = JdbcUserNotificationBulkWriterAdapter(jdbcTemplate)

        "알림을 500건 단위의 multi-row INSERT로 저장한다" {
            every { jdbcTemplate.update(any<String>(), any<PreparedStatementSetter>()) } returns 1
            val user = UserTestFixture.createActiveUser1(1L)
            val notifications = List(501) { createNotification(user) }

            adapter.saveAll(notifications)

            verify(exactly = 2) { jdbcTemplate.update(any<String>(), any<PreparedStatementSetter>()) }
        }
    }) {
    private companion object {
        fun createNotification(user: com.weeth.domain.user.domain.entity.User): UserNotification =
            UserNotification.create(
                user = user,
                type = NotificationType.NOTICE_CREATED,
                title = "새 공지가 등록되었습니다",
                body = "중간고사 기간 공지",
                targetPath = "/clubs/10/boards/10/posts/100",
                clubId = 62L,
                boardId = 10L,
                postId = 100L,
            )
    }
}
