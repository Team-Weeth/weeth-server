package com.weeth.global.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer
import java.time.Duration

/*
    Spring Cache 추상화(@Cacheable)를 Redis와 연결하는 설정
    키: String, 값: JSON 직렬화, 기본 TTL: 7일
 */
@EnableCaching
@Configuration
class CacheConfig(
    private val redisConnectionFactory: RedisConnectionFactory,
    private val objectMapper: ObjectMapper,
) {
    @Bean
    fun cacheManager(): RedisCacheManager {
        // Spring Boot 자동 구성 ObjectMapper(KotlinModule 포함)를 기반으로
        // 타입 정보(@class)를 포함한 Redis 전용 ObjectMapper 생성
        val redisObjectMapper =
            objectMapper.copy().activateDefaultTyping(
                BasicPolymorphicTypeValidator
                    .builder()
                    .allowIfSubType("com.weeth.")
                    .allowIfSubType("java.util.")
                    .build(),
                ObjectMapper.DefaultTyping.NON_FINAL,
            )

        val defaultConfig =
            RedisCacheConfiguration
                .defaultCacheConfig()
                .entryTtl(Duration.ofDays(7))
                .serializeKeysWith(
                    RedisSerializationContext.SerializationPair.fromSerializer(StringRedisSerializer()),
                ).serializeValuesWith(
                    RedisSerializationContext.SerializationPair.fromSerializer(
                        GenericJackson2JsonRedisSerializer(redisObjectMapper),
                    ),
                )

        return RedisCacheManager
            .builder(redisConnectionFactory)
            .cacheDefaults(defaultConfig)
            .build()
    }
}
