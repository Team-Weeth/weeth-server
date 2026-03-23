package com.weeth.global.auth.jwt.application.usecase

import com.weeth.global.auth.jwt.application.dto.JwtDto
import com.weeth.global.auth.jwt.application.exception.InvalidTokenException
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
    ): JwtDto {
        val accessToken = jwtTokenProvider.createAccessToken(userId, email)
        val refreshToken = jwtTokenProvider.createRefreshToken(userId)

        refreshTokenStore.save(userId, refreshToken, email)

        return JwtDto(accessToken, refreshToken)
    }

    fun reIssueToken(requestToken: String): JwtDto {
        jwtTokenProvider.validate(requestToken)

        val userId = jwtTokenExtractor.extractId(requestToken) ?: throw InvalidTokenException()
        refreshTokenStore.validateRefreshToken(userId, requestToken)

        val email = refreshTokenStore.getEmail(userId)

        return create(userId, email)
    }
}
