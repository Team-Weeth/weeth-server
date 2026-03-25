package com.weeth.global.auth.jwt.application.service

import com.weeth.global.config.properties.CookieProperties
import com.weeth.global.config.properties.JwtProperties
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class TokenCookieProvider(
    private val cookieProperties: CookieProperties,
    private val jwtProperties: JwtProperties,
) {
    fun createAccessTokenCookie(token: String): ResponseCookie =
        buildCookie(
            name = cookieProperties.accessTokenName,
            value = token,
            maxAge = Duration.ofMillis(jwtProperties.access.expiration),
            path = cookieProperties.path,
        )

    fun createRefreshTokenCookie(token: String): ResponseCookie =
        buildCookie(
            name = cookieProperties.refreshTokenName,
            value = token,
            maxAge = Duration.ofMillis(jwtProperties.refresh.expiration),
            path = cookieProperties.refreshPath,
        )

    private fun buildCookie(
        name: String,
        value: String,
        maxAge: Duration,
        path: String,
    ): ResponseCookie =
        ResponseCookie
            .from(name, value)
            .httpOnly(cookieProperties.httpOnly)
            .secure(cookieProperties.secure)
            .path(path)
            .maxAge(maxAge)
            .sameSite(cookieProperties.sameSite)
            .apply {
                if (cookieProperties.domain.isNotBlank()) {
                    domain(cookieProperties.domain)
                }
            }.build()
}
