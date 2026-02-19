package com.weeth.global.auth.jwt.application.service

import com.weeth.domain.user.domain.entity.enums.Role
import com.weeth.global.auth.jwt.application.exception.InvalidTokenException
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

    fun extractEmail(accessToken: String): String? =
        runCatching {
            val claims: Claims = jwtTokenProvider.parseClaims(accessToken)
            claims.get(JwtTokenProvider.EMAIL_CLAIM, String::class.java)
        }.getOrElse {
            log.error("액세스 토큰이 유효하지 않습니다.")
            null
        }

    fun extractId(token: String): Long? =
        runCatching {
            val claims: Claims = jwtTokenProvider.parseClaims(token)
            claims.get(JwtTokenProvider.ID_CLAIM, Long::class.java)
        }.getOrElse {
            log.error("액세스 토큰이 유효하지 않습니다.")
            null
        }

    fun extractClaims(token: String): TokenClaims? =
        runCatching {
            val claims: Claims = jwtTokenProvider.parseClaims(token)
            TokenClaims(
                id = claims.get(JwtTokenProvider.ID_CLAIM, Long::class.java),
                email = claims.get(JwtTokenProvider.EMAIL_CLAIM, String::class.java),
                role =
                    runCatching { Role.valueOf(claims.get(JwtTokenProvider.ROLE_CLAIM, String::class.java)) }
                        .getOrElse { throw InvalidTokenException() },
            )
        }.getOrElse {
            log.error("액세스 토큰이 유효하지 않습니다.")
            null
        }

    companion object {
        private const val BEARER = "Bearer "
    }
}
