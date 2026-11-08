package com.weeth.domain.notification.domain.repository

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.springframework.core.io.ClassPathResource
import org.springframework.jdbc.datasource.init.ScriptUtils
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.utility.DockerImageName
import java.sql.DriverManager

class NotificationTemplateMigrationTest :
    StringSpec({
        val mysql = MySQLContainer<Nothing>(DockerImageName.parse("mysql:8.0.41"))
        beforeSpec { mysql.start() }
        afterSpec { mysql.stop() }

        "V24는 기존 알림 데이터를 보존하며 nullable 버전과 실패 추적 테이블을 추가한다" {
            DriverManager.getConnection(mysql.jdbcUrl, mysql.username, mysql.password).use { connection ->
                connection.createStatement().use { statement ->
                    statement.execute(
                        "CREATE TABLE user_notification (user_notification_id BIGINT PRIMARY KEY, body VARCHAR(255))",
                    )
                    statement.execute("INSERT INTO user_notification VALUES (1, '기존 알림')")
                }
                ScriptUtils.executeSqlScript(
                    connection,
                    ClassPathResource("db/migration/V24__add_notification_templates_and_failures.sql"),
                )
                connection.createStatement().use { statement ->
                    statement
                        .executeQuery(
                            "SELECT body, template_version FROM user_notification WHERE user_notification_id=1",
                        ).use {
                            it.next() shouldBe true
                            it.getString("body") shouldBe "기존 알림"
                            it.getObject("template_version") shouldBe null
                        }
                    statement.execute(
                        """
                        INSERT INTO notification_failure (event_id, club_id, type, channel, template_version, reason, missing_variables)
                        VALUES ('NOTICE_CREATED:1:1', 1, 'NOTICE_CREATED', 'PUSH', 2, 'MISSING_VARIABLES', 'noticeTitle')
                        """.trimIndent(),
                    )
                    statement.executeQuery("SELECT event_id, reason, template_version FROM notification_failure").use {
                        it.next() shouldBe true
                        it.getString("event_id") shouldBe "NOTICE_CREATED:1:1"
                        it.getString("reason") shouldBe "MISSING_VARIABLES"
                        it.getInt("template_version") shouldBe 2
                    }
                }
            }
        }
    })
