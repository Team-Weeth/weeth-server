package com.weeth.global.auth.jwt.application.service

import com.weeth.domain.user.domain.entity.enums.Role
import com.weeth.global.auth.jwt.application.exception.TokenNotFoundException
import com.weeth.global.auth.jwt.domain.service.JwtTokenProvider
import com.weeth.global.config.properties.JwtProperties
import io.jsonwebtoken.Claims
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class JwtTokenExtractor(
    private val jwtProperties: JwtProperties,
    private val jwtTokenProvider: JwtTokenProvider,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    data class TokenClaims(
        val id: Long,
        val email: String,
        val role: Role,
    )

    fun extractRefreshToken(request: HttpServletRequest): String =
        request
            .getHeader(jwtProperties.refresh.header)
            ?.takeIf { it.startsWith(BEARER) }
            ?.removePrefix(BEARER)
            ?: throw TokenNotFoundException()

    fun extractAccessToken(request: HttpServletRequest): String? =
        request
            .getHeader(jwtProperties.access.header)
            ?.takeIf { it.startsWith(BEARER) }
            ?.removePrefix(BEARER)

    fun extractEmail(accessToken: String): String? = extractClaim(accessToken, JwtTokenProvider.EMAIL_CLAIM, String::class.java)

    fun extractId(token: String): Long? = extractClaim(token, JwtTokenProvider.ID_CLAIM, Long::class.java)

    fun extractClaims(token: String): TokenClaims? =
        runCatching {
            val claims: Claims = jwtTokenProvider.parseClaims(token)
            TokenClaims(
                id = claims.get(JwtTokenProvider.ID_CLAIM, Long::class.java),
                email = claims.get(JwtTokenProvider.EMAIL_CLAIM, String::class.java),
                role = Role.valueOf(claims.get(JwtTokenProvider.ROLE_CLAIM, String::class.java)),
            )
        }.onFailure {
            log.error("액세스 토큰이 유효하지 않습니다: {}", it.message)
        }.getOrNull()

    private fun <T> extractClaim(
        token: String,
        claimName: String,
        type: Class<T>,
    ): T? =
        runCatching {
            jwtTokenProvider.parseClaims(token).get(claimName, type)
        }.onFailure {
            log.error("액세스 토큰 claim 추출 실패({}): {}", claimName, it.message)
        }.getOrNull()

    companion object {
        private const val BEARER = "Bearer "
    }
}
