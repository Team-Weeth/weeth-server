package com.weeth.global.config

import com.weeth.global.config.properties.RedisProperties
import io.lettuce.core.metrics.MicrometerCommandLatencyRecorder
import io.lettuce.core.metrics.MicrometerOptions
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.boot.autoconfigure.data.redis.LettuceClientConfigurationBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.RedisKeyValueAdapter
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories
import org.springframework.data.redis.serializer.StringRedisSerializer

@Configuration
@EnableRedisRepositories(enableKeyspaceEvents = RedisKeyValueAdapter.EnableKeyspaceEvents.ON_STARTUP)
class RedisConfig(
    private val redisProperties: RedisProperties,
    private val meterRegistry: MeterRegistry,
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

        val clientConfig =
            LettuceClientConfiguration
                .builder()
                .clientResources(
                    io.lettuce.core.resource.ClientResources
                        .builder()
                        .commandLatencyRecorder(
                            MicrometerCommandLatencyRecorder(meterRegistry, MicrometerOptions.defaults()),
                        ).build(),
                ).build()

        return LettuceConnectionFactory(redisConfiguration, clientConfig)
    }

    @Bean
    fun redisTemplate(redisConnectionFactory: RedisConnectionFactory): RedisTemplate<String, String> =
        RedisTemplate<String, String>().apply {
            keySerializer = StringRedisSerializer()
            valueSerializer = StringRedisSerializer()
            connectionFactory = redisConnectionFactory
        }
}
