package com.weeth.domain.notification.infrastructure

import com.weeth.domain.notification.domain.entity.UserNotification
import com.weeth.domain.notification.domain.enums.NotificationType
import com.weeth.domain.user.fixture.UserTestFixture
import io.kotest.core.spec.style.StringSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.PreparedStatementSetter
import java.sql.PreparedStatement

class JdbcUserNotificationBulkWriterAdapterTest :
    StringSpec({
        val jdbcTemplate = mockk<JdbcTemplate>()
        val adapter = JdbcUserNotificationBulkWriterAdapter(jdbcTemplate)

        beforeTest { clearMocks(jdbcTemplate) }

        "알림을 500건 단위의 multi-row INSERT로 저장한다" {
            every { jdbcTemplate.update(any<String>(), any<PreparedStatementSetter>()) } returns 1
            val user = UserTestFixture.createActiveUser1(1L)
            val notifications = List(501) { createNotification(user) }

            adapter.saveAll(notifications)

            verify(exactly = 2) { jdbcTemplate.update(any<String>(), any<PreparedStatementSetter>()) }
        }

        "렌더링에 사용한 템플릿 버전을 INSERT에 저장한다" {
            val setter = slot<PreparedStatementSetter>()
            val statement = mockk<PreparedStatement>(relaxed = true)
            every { jdbcTemplate.update(match<String> { "template_version" in it }, capture(setter)) } returns 1
            adapter.saveAll(listOf(createNotification(UserTestFixture.createActiveUser1(1L), 3)))
            setter.captured.setValues(statement)
            verify { statement.setInt(11, 3) }
        }
    }) {
    private companion object {
        fun createNotification(
            user: com.weeth.domain.user.domain.entity.User,
            templateVersion: Int? = null,
        ): UserNotification =
            UserNotification.create(
                user = user,
                type = NotificationType.NOTICE_CREATED,
                title = "새 공지가 등록되었습니다",
                body = "중간고사 기간 공지",
                targetPath = "/clubs/10/boards/10/posts/100",
                clubId = 62L,
                referenceType = com.weeth.domain.notification.domain.enums.NotificationReferenceType.POST,
                referenceId = 100L,
                templateVersion = templateVersion,
            )
    }
}
