package com.weeth.global.config

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
    키: String, 값: JSON 직렬화, 기본 TTL: 24시간
 */
@EnableCaching
@Configuration
class CacheConfig(
    private val redisConnectionFactory: RedisConnectionFactory,
) {
    @Bean
    fun cacheManager(): RedisCacheManager {
        val defaultConfig =
            RedisCacheConfiguration
                .defaultCacheConfig()
                .entryTtl(Duration.ofHours(24))
                .serializeKeysWith(
                    RedisSerializationContext.SerializationPair.fromSerializer(StringRedisSerializer()),
                ).serializeValuesWith(
                    RedisSerializationContext.SerializationPair.fromSerializer(GenericJackson2JsonRedisSerializer()),
                )

        return RedisCacheManager
            .builder(redisConnectionFactory)
            .cacheDefaults(defaultConfig)
            .build()
    }
}
