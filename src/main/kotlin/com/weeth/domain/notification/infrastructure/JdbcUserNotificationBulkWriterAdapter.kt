package com.weeth.domain.notification.infrastructure

import com.weeth.domain.notification.domain.entity.UserNotification
import com.weeth.domain.notification.domain.port.UserNotificationBulkWriter
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.PreparedStatementSetter
import org.springframework.stereotype.Component
import java.sql.Types

@Component
class JdbcUserNotificationBulkWriterAdapter(
    private val jdbcTemplate: JdbcTemplate,
) : UserNotificationBulkWriter {
    override fun saveAll(notifications: List<UserNotification>) {
        if (notifications.isEmpty()) {
            return
        }

        notifications.chunked(BATCH_SIZE).forEach { batch ->
            val values = List(batch.size) { ROW_PLACEHOLDERS }.joinToString(", ")
            jdbcTemplate.update(
                "$INSERT_SQL $values",
                PreparedStatementSetter { statement ->
                    var parameterIndex = 1
                    batch.forEach { notification ->
                        statement.setLong(parameterIndex++, notification.user.id)
                        statement.setString(parameterIndex++, notification.type.name)
                        statement.setString(parameterIndex++, notification.title)
                        statement.setString(parameterIndex++, notification.body)
                        statement.setString(parameterIndex++, notification.targetPath)
                        statement.setLong(parameterIndex++, notification.clubId)
                        statement.setString(parameterIndex++, notification.referenceType.name)
                        statement.setLong(parameterIndex++, notification.referenceId)
                        statement.setBoolean(parameterIndex++, notification.isRead)
                        notification.readAt?.let { statement.setObject(parameterIndex++, it) }
                            ?: statement.setNull(parameterIndex++, Types.TIMESTAMP)
                    }
                },
            )
        }
    }

    private companion object {
        const val BATCH_SIZE = 500
        const val INSERT_SQL =
            """
            INSERT INTO user_notification (
                user_id, type, title, body, target_path,
                club_id, reference_type, reference_id, is_read, read_at,
                created_at, modified_at
            ) VALUES
            """
        const val ROW_PLACEHOLDERS =
            "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))"
    }
}
