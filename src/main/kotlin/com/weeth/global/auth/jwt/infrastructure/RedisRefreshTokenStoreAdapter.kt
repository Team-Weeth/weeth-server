package com.weeth.global.auth.jwt.infrastructure

import com.weeth.global.auth.jwt.application.exception.InvalidTokenException
import com.weeth.global.auth.jwt.application.exception.RedisTokenNotFoundException
import com.weeth.global.auth.jwt.domain.enums.TokenType
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
        email: String,
        tokenType: TokenType,
    ) {
        val key = getKey(userId)
        redisTemplate.opsForHash<String, String>().putAll(
            key,
            mapOf(
                TOKEN to refreshToken,
                EMAIL to email,
                TOKEN_TYPE to tokenType.name,
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

    override fun getTokenType(userId: Long): TokenType {
        val key = getKey(userId)
        val value =
            redisTemplate.opsForHash<String, String>().get(key, TOKEN_TYPE)
                ?: return TokenType.ACCESS // 기존 토큰 호환성을 위한 기본값
        return TokenType.valueOf(value)
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
        private const val EMAIL = "email"
        private const val TOKEN_TYPE = "tokenType"
    }
}
