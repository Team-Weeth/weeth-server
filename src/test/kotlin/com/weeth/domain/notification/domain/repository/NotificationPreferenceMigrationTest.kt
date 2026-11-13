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

class NotificationPreferenceMigrationTest :
    StringSpec({
        val mysql = MySQLContainer<Nothing>(DockerImageName.parse("mysql:8.0.41"))
        beforeSpec { mysql.start() }
        afterSpec { mysql.stop() }

        "V23 migration은 MySQL에서 기본값·복합 유니크 키·참조 무결성을 적용한다" {
            DriverManager.getConnection(mysql.jdbcUrl, mysql.username, mysql.password).use { connection ->
                connection.createStatement().use { statement ->
                    statement.execute("CREATE TABLE users (user_id BIGINT NOT NULL PRIMARY KEY)")
                    statement.execute("CREATE TABLE club (club_id BIGINT NOT NULL PRIMARY KEY)")
                    statement.execute("INSERT INTO users VALUES (1), (2)")
                    statement.execute("INSERT INTO club VALUES (10), (20)")
                }
                ScriptUtils.executeSqlScript(
                    connection,
                    ClassPathResource("db/migration/V23__add_notification_preferences.sql"),
                )
                connection.createStatement().use { statement ->
                    statement.execute("INSERT INTO notification_setting (user_id, club_id) VALUES (1, 10)")
                    statement
                        .executeQuery(
                            "SELECT master_enabled FROM notification_setting WHERE user_id=1 AND club_id=10",
                        ).use {
                            it.next() shouldBe true
                            it.getBoolean(1) shouldBe true
                        }
                    shouldThrow<SQLException> {
                        statement.execute("INSERT INTO notification_setting (user_id, club_id) VALUES (1, 10)")
                    }
                    statement.execute("INSERT INTO notification_setting (user_id, club_id) VALUES (1, 20), (2, 10)")
                    statement.execute(
                        "INSERT INTO notification_preference (user_id, club_id, category) VALUES (1, 10, 'NOTICE')",
                    )
                    statement
                        .executeQuery(
                            "SELECT enabled FROM notification_preference WHERE user_id=1 AND club_id=10",
                        ).use {
                            it.next() shouldBe true
                            it.getBoolean(1) shouldBe true
                        }
                    shouldThrow<SQLException> {
                        statement.execute(
                            "INSERT INTO notification_preference (user_id, club_id, category) VALUES (1, 10, 'NOTICE')",
                        )
                    }
                    statement.execute(
                        "INSERT INTO notification_preference (user_id, club_id, category) VALUES (1, 10, 'SCHEDULE')",
                    )
                    shouldThrow<SQLException> {
                        statement.execute("INSERT INTO notification_setting (user_id, club_id) VALUES (99, 10)")
                    }
                    shouldThrow<SQLException> {
                        statement.execute(
                            "INSERT INTO notification_preference (user_id, club_id, category) VALUES (1, 99, 'NOTICE')",
                        )
                    }
                }
            }
        }
    })
