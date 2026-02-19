package com.weeth.global.auth.jwt.domain.service

import com.weeth.domain.user.domain.entity.enums.Role
import com.weeth.global.auth.jwt.application.exception.InvalidTokenException
import com.weeth.global.config.properties.JwtProperties
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class JwtTokenProviderTest :
    StringSpec({
        val jwtProperties =
            JwtProperties(
                key = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
                access = JwtProperties.TokenProperties(expiration = 60_000L, header = "Authorization"),
                refresh = JwtProperties.TokenProperties(expiration = 120_000L, header = "Authorization_refresh"),
            )

        val jwtProvider = JwtTokenProvider(jwtProperties)

        "access token 생성 후 claims를 파싱할 수 있다" {
            val token = jwtProvider.createAccessToken(1L, "test@weeth.com", Role.ADMIN)

            val claims = jwtProvider.parseClaims(token)

            claims.get("id", Number::class.java).toLong() shouldBe 1L
            claims.get("email", String::class.java) shouldBe "test@weeth.com"
            claims.get("role", String::class.java) shouldBe "ADMIN"
        }

        "유효하지 않은 토큰 검증 시 InvalidTokenException이 발생한다" {
            shouldThrow<InvalidTokenException> {
                jwtProvider.validate("not-a-token")
            }
        }
    })
