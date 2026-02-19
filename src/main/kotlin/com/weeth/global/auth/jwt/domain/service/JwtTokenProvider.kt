package com.weeth.global.auth.jwt.domain.service

import com.weeth.global.auth.jwt.application.exception.InvalidTokenException
import com.weeth.global.config.properties.JwtProperties
import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.JwtParser
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.util.Date
import javax.crypto.SecretKey

@Service
class JwtTokenProvider(
    jwtProperties: JwtProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val secretKey: SecretKey = Keys.hmacShaKeyFor(jwtProperties.key.toByteArray(StandardCharsets.UTF_8))
    private val accessTokenExpirationPeriod: Long = jwtProperties.access.expiration
    private val refreshTokenExpirationPeriod: Long = jwtProperties.refresh.expiration
    private val jwtParser: JwtParser =
        Jwts
            .parser()
            .verifyWith(secretKey)
            .build()

    fun createAccessToken(
        id: Long,
        email: String,
        role: String,
    ): String {
        val now = Date()
        return Jwts
            .builder()
            .subject(ACCESS_TOKEN_SUBJECT)
            .claim(ID_CLAIM, id)
            .claim(EMAIL_CLAIM, email)
            .claim(ROLE_CLAIM, role)
            .issuedAt(now)
            .expiration(Date(now.time + accessTokenExpirationPeriod))
            .signWith(secretKey)
            .compact()
    }

    fun createRefreshToken(id: Long): String {
        val now = Date()
        return Jwts
            .builder()
            .subject(REFRESH_TOKEN_SUBJECT)
            .claim(ID_CLAIM, id)
            .issuedAt(now)
            .expiration(Date(now.time + refreshTokenExpirationPeriod))
            .signWith(secretKey)
            .compact()
    }

    fun validate(token: String) {
        parseSignedClaims(token, "유효하지 않은 토큰입니다.")
    }

    fun parseClaims(token: String): Claims =
        parseSignedClaims(token, "토큰 파싱 실패")
            .payload

    private fun parseSignedClaims(
        token: String,
        errorMessage: String,
    ) = try {
        jwtParser.parseSignedClaims(token)
    } catch (e: JwtException) {
        log.error("{}: {}", errorMessage, e.message)
        throw InvalidTokenException()
    } catch (e: IllegalArgumentException) {
        log.error("{}: {}", errorMessage, e.message)
        throw InvalidTokenException()
    }

    companion object {
        private const val ACCESS_TOKEN_SUBJECT = "AccessToken"
        private const val REFRESH_TOKEN_SUBJECT = "RefreshToken"
        internal const val EMAIL_CLAIM = "email"
        internal const val ID_CLAIM = "id"
        internal const val ROLE_CLAIM = "role"
    }
}
