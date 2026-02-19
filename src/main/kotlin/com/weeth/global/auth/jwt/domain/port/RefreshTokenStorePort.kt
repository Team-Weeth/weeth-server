package com.weeth.global.auth.jwt.domain.port

import com.weeth.domain.user.domain.entity.enums.Role

interface RefreshTokenStorePort {
    fun save(
        userId: Long,
        refreshToken: String,
        role: Role,
        email: String,
    )

    fun delete(userId: Long)

    fun validateRefreshToken(
        userId: Long,
        requestToken: String,
    )

    fun getEmail(userId: Long): String

    fun getRole(userId: Long): Role

    fun updateRole(
        userId: Long,
        role: Role,
    )
}
