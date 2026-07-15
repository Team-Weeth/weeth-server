package com.weeth.global.auth.jwt.infrastructure

import com.weeth.global.auth.jwt.domain.port.AccessTokenBlacklistStorePort
import com.weeth.global.config.properties.JwtProperties
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class RedisAccessTokenBlacklistStoreAdapter(
    private val jwtProperties: JwtProperties,
    private val redisTemplate: RedisTemplate<String, String>,
) : AccessTokenBlacklistStorePort {
    override fun blacklist(userId: Long) {
        redisTemplate
            .opsForValue()
            .set(
                getKey(userId),
                BLACKLISTED,
                jwtProperties.access.expiration + TTL_BUFFER_MILLIS,
                TimeUnit.MILLISECONDS,
            )
    }

    override fun isBlacklisted(userId: Long): Boolean = redisTemplate.hasKey(getKey(userId)) == true

    private fun getKey(userId: Long): String = "$PREFIX$userId"

    companion object {
        private const val PREFIX = "accessTokenBlacklist:"
        private const val BLACKLISTED = "true"
        private const val TTL_BUFFER_MILLIS = 60_000L
    }
}
