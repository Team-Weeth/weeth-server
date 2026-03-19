package com.weeth.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.utility.DockerImageName

@TestConfiguration
class TestContainersConfig {
    @Bean
    @ServiceConnection
    fun mysqlContainer(): MySQLContainer<*> = MySQLContainer(DockerImageName.parse(MYSQL_IMAGE))

    @Bean
    @ServiceConnection(name = "redis")
    fun redisContainer(): GenericContainer<*> =
        GenericContainer(DockerImageName.parse(REDIS_IMAGE))
            .withExposedPorts(6379)

    companion object {
        private const val MYSQL_IMAGE = "mysql:8.0.41"
        private const val REDIS_IMAGE = "redis:7.2.7"
    }
}
