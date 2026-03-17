package com.weeth.global.auth.jwt.domain.port

interface RefreshTokenStorePort {
    fun save(
        userId: Long,
        refreshToken: String,
        email: String,
    )

    fun delete(userId: Long)

    fun validateRefreshToken(
        userId: Long,
        requestToken: String,
    )

    fun getEmail(userId: Long): String
}
