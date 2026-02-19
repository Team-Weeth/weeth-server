package com.weeth.global.auth.jwt.application.usecase

import com.weeth.global.auth.jwt.application.dto.JwtDto
import com.weeth.global.auth.jwt.application.service.JwtTokenExtractor
import com.weeth.global.auth.jwt.domain.port.RefreshTokenStorePort
import com.weeth.global.auth.jwt.domain.service.JwtTokenProvider
import org.springframework.stereotype.Service

@Service
class JwtManageUseCase(
    private val jwtTokenProvider: JwtTokenProvider,
    private val jwtTokenExtractor: JwtTokenExtractor,
    private val refreshTokenStore: RefreshTokenStorePort,
) {
    fun create(
        userId: Long,
        email: String,
        role: String,
    ): JwtDto {
        val accessToken = jwtTokenProvider.createAccessToken(userId, email, role)
        val refreshToken = jwtTokenProvider.createRefreshToken(userId)

        updateToken(userId, refreshToken, role, email)

        return JwtDto(accessToken, refreshToken)
    }

    fun reIssueToken(requestToken: String): JwtDto {
        jwtTokenProvider.validate(requestToken)

        val userId = requireNotNull(jwtTokenExtractor.extractId(requestToken))
        refreshTokenStore.validateRefreshToken(userId, requestToken)

        val role = refreshTokenStore.getRole(userId)
        val email = refreshTokenStore.getEmail(userId)

        return create(userId, email, role)
    }

    private fun updateToken(
        userId: Long,
        refreshToken: String,
        role: String,
        email: String,
    ) {
        refreshTokenStore.save(userId, refreshToken, role, email)
    }
}
