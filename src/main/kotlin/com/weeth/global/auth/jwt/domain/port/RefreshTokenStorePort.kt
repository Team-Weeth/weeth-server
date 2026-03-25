package com.weeth.global.auth.jwt.domain.port

import com.weeth.global.auth.jwt.domain.enums.TokenType

interface RefreshTokenStorePort {
    fun save(
        userId: Long,
        refreshToken: String,
        email: String,
        tokenType: TokenType,
    )

    fun delete(userId: Long)

    fun validateRefreshToken(
        userId: Long,
        requestToken: String,
    )

    fun getEmail(userId: Long): String

    fun getTokenType(userId: Long): TokenType
}
