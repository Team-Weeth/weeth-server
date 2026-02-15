package com.weeth.config

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldNotBeNull
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.testcontainers.containers.MySQLContainer

@DataJpaTest
@Import(TestContainersConfig::class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TestContainersTest(
    private val mysqlContainer: MySQLContainer<*>,
) : StringSpec({

        "설정파일로 주입된 컨테이너 정상 동작 테스트" {
            mysqlContainer.shouldNotBeNull()
            mysqlContainer.isRunning.shouldBeTrue()
        }
    })
