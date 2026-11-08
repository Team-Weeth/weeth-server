package com.weeth.domain.notification.domain.repository

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.springframework.core.io.ClassPathResource
import org.springframework.jdbc.datasource.init.ScriptUtils
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.utility.DockerImageName
import java.sql.DriverManager
import java.sql.SQLException

class NotificationDispatchMigrationTest :
    StringSpec({
        val mysql = MySQLContainer<Nothing>(DockerImageName.parse("mysql:8.0.41"))
        beforeSpec { mysql.start() }
        afterSpec { mysql.stop() }

        "V25는 기존 알림을 보존하며 이벤트·사용자·채널 및 Outbox·토큰 중복을 제한한다" {
            DriverManager.getConnection(mysql.jdbcUrl, mysql.username, mysql.password).use { connection ->
                connection.createStatement().use { statement ->
                    statement.execute(
                        "CREATE TABLE user_notification (user_notification_id BIGINT PRIMARY KEY, user_id BIGINT NOT NULL)",
                    )
                    statement.execute("INSERT INTO user_notification VALUES (1, 10), (2, 10)")
                }
                ScriptUtils.executeSqlScript(
                    connection,
                    ClassPathResource("db/migration/V25__add_notification_outbox_and_delivery.sql"),
                )
                connection.createStatement().use { statement ->
                    statement.executeQuery("SELECT COUNT(*) FROM user_notification WHERE event_id IS NULL").use {
                        it.next()
                        it.getInt(1) shouldBe
                            2
                    }
                    statement.execute("UPDATE user_notification SET event_id='event' WHERE user_notification_id=1")
                    shouldThrow<SQLException> {
                        statement.execute("UPDATE user_notification SET event_id='event' WHERE user_notification_id=2")
                    }
                    statement.executeUpdate(
                        "INSERT IGNORE INTO notification_event (event_id, club_id) VALUES ('event', 1)",
                    ) shouldBe
                        1
                    statement.executeUpdate(
                        "INSERT IGNORE INTO notification_event (event_id, club_id) VALUES ('event', 1)",
                    ) shouldBe
                        0
                    val insertDelivery =
                        """
                        INSERT INTO notification_delivery
                        (event_id, user_id, club_id, type, channel, title, body, template_version, payload_data, status, attempt_count, targets_initialized, next_attempt_at)
                        VALUES ('event', 10, 1, 'NOTICE_CREATED', 'PUSH', '제목', '본문', 1, '{}', 'PENDING', 0, false, CURRENT_TIMESTAMP(6))
                        """.trimIndent()
                    statement.execute(insertDelivery)
                    shouldThrow<SQLException> { statement.execute(insertDelivery) }
                    val deliveryId =
                        statement
                            .executeQuery(
                                "SELECT notification_delivery_id FROM notification_delivery",
                            ).use {
                                it.next()
                                it.getLong(1)
                            }
                    val insertOutbox =
                        """
                        INSERT INTO notification_outbox (delivery_id, event_id, status, attempt_count, next_attempt_at)
                        VALUES ($deliveryId, 'event', 'PENDING', 0, CURRENT_TIMESTAMP(6))
                        """.trimIndent()
                    statement.execute(insertOutbox)
                    shouldThrow<SQLException> { statement.execute(insertOutbox) }
                    val insertTarget =
                        """
                        INSERT INTO notification_delivery_target (delivery_id, token_id, registered_at, status)
                        VALUES ($deliveryId, 20, CURRENT_TIMESTAMP(6), 'PENDING')
                        """.trimIndent()
                    statement.execute(insertTarget)
                    shouldThrow<SQLException> { statement.execute(insertTarget) }
                    shouldThrow<SQLException> {
                        statement.execute(
                            "INSERT INTO notification_delivery_target (delivery_id, token_id, registered_at, status) VALUES (99999, 20, CURRENT_TIMESTAMP(6), 'PENDING')",
                        )
                    }
                }
            }
        }
    })
