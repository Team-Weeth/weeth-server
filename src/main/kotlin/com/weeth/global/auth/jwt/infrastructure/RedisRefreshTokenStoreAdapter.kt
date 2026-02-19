package com.weeth.global.auth.jwt.infrastructure

import com.weeth.global.auth.jwt.application.exception.InvalidTokenException
import com.weeth.global.auth.jwt.application.exception.RedisTokenNotFoundException
import com.weeth.global.auth.jwt.domain.port.RefreshTokenStorePort
import com.weeth.global.config.properties.JwtProperties
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class RedisRefreshTokenStoreAdapter(
    private val jwtProperties: JwtProperties,
    private val redisTemplate: RedisTemplate<String, String>,
) : RefreshTokenStorePort {
    override fun save(
        userId: Long,
        refreshToken: String,
        role: String,
        email: String,
    ) {
        val key = getKey(userId)
        redisTemplate.opsForHash<String, String>().putAll(
            key,
            mapOf(
                TOKEN to refreshToken,
                ROLE to role,
                EMAIL to email,
            ),
        )
        redisTemplate.expire(key, jwtProperties.refresh.expiration, TimeUnit.MINUTES)
    }

    override fun delete(userId: Long) {
        val key = getKey(userId)
        redisTemplate.delete(key)
    }

    override fun validateRefreshToken(
        userId: Long,
        requestToken: String,
    ) {
        if (find(userId) != requestToken) {
            throw InvalidTokenException()
        }
    }

    override fun getEmail(userId: Long): String {
        val key = getKey(userId)
        return redisTemplate.opsForHash<String, String>().get(key, EMAIL)
            ?: throw RedisTokenNotFoundException()
    }

    override fun getRole(userId: Long): String {
        val key = getKey(userId)
        return redisTemplate.opsForHash<String, String>().get(key, ROLE)
            ?: throw RedisTokenNotFoundException()
    }

    override fun updateRole(
        userId: Long,
        role: String,
    ) {
        val key = getKey(userId)
        if (redisTemplate.hasKey(key) == true) {
            redisTemplate.opsForHash<String, String>().put(key, ROLE, role)
        }
    }

    private fun find(userId: Long): String {
        val key = getKey(userId)
        return redisTemplate.opsForHash<String, String>().get(key, TOKEN)
            ?: throw RedisTokenNotFoundException()
    }

    private fun getKey(userId: Long): String = "$PREFIX$userId"

    companion object {
        private const val PREFIX = "refreshToken:"
        private const val TOKEN = "token"
        private const val ROLE = "role"
        private const val EMAIL = "email"
    }
}
