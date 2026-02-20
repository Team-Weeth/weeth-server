package com.weeth.global.config

import com.weeth.global.config.properties.RedisProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.RedisKeyValueAdapter
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories
import org.springframework.data.redis.serializer.StringRedisSerializer

@Configuration
@EnableRedisRepositories(enableKeyspaceEvents = RedisKeyValueAdapter.EnableKeyspaceEvents.ON_STARTUP)
class RedisConfig(
    private val redisProperties: RedisProperties,
) {
    @Bean
    fun redisConnectionFactory(): RedisConnectionFactory {
        val redisConfiguration =
            RedisStandaloneConfiguration().apply {
                hostName = redisProperties.host
                port = redisProperties.port
                if (!redisProperties.password.isNullOrEmpty()) {
                    setPassword(redisProperties.password)
                }
            }

        return LettuceConnectionFactory(redisConfiguration)
    }

    @Bean
    fun redisTemplate(): RedisTemplate<String, String> =
        RedisTemplate<String, String>().apply {
            keySerializer = StringRedisSerializer()
            valueSerializer = StringRedisSerializer()
            connectionFactory = redisConnectionFactory()
        }
}
