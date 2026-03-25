package com.weeth.global.auth.jwt.application.service

import com.weeth.global.auth.jwt.application.exception.TokenNotFoundException
import com.weeth.global.auth.jwt.domain.service.JwtTokenProvider
import com.weeth.global.config.properties.CookieProperties
import com.weeth.global.config.properties.JwtProperties
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest

class JwtTokenExtractorTest :
    DescribeSpec({
        val jwtProperties =
            JwtProperties(
                key = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
                access = JwtProperties.TokenProperties(expiration = 60_000L, header = "Auth"),
                refresh = JwtProperties.TokenProperties(expiration = 120_000L, header = "Refresh"),
            )

        val cookieProperties = CookieProperties(accessTokenName = "access_token", refreshTokenName = "refresh_token")
        val jwtProvider = mockk<JwtTokenProvider>()
        val jwtTokenExtractor = JwtTokenExtractor(jwtProperties, jwtProvider, cookieProperties)

        beforeTest {
            clearMocks(jwtProvider)
        }

        describe("extractAccessToken") {
            it("Bearer 헤더에서 access token을 추출한다") {
                val request = mockk<HttpServletRequest>()
                every { request.getHeader("Auth") } returns "Bearer access-token"

                val token = jwtTokenExtractor.extractAccessToken(request)

                token shouldBe "access-token"
            }
        }

        describe("extractRefreshToken") {
            it("Cookie에서 refresh token을 우선 추출한다") {
                val request = mockk<HttpServletRequest>()
                every { request.cookies } returns arrayOf(Cookie("refresh_token", "cookie-refresh-token"))

                val token = jwtTokenExtractor.extractRefreshToken(request)

                token shouldBe "cookie-refresh-token"
            }

            it("Cookie가 없으면 Header에서 refresh token을 추출한다") {
                val request = mockk<HttpServletRequest>()
                every { request.cookies } returns null
                every { request.getHeader("Refresh") } returns "Bearer header-refresh-token"

                val token = jwtTokenExtractor.extractRefreshToken(request)

                token shouldBe "header-refresh-token"
            }

            it("Cookie가 빈 값이면 Header에서 refresh token을 추출한다") {
                val request = mockk<HttpServletRequest>()
                every { request.cookies } returns arrayOf(Cookie("refresh_token", ""))
                every { request.getHeader("Refresh") } returns "Bearer header-refresh-token"

                val token = jwtTokenExtractor.extractRefreshToken(request)

                token shouldBe "header-refresh-token"
            }

            it("Cookie와 Header 둘 다 없으면 TokenNotFoundException이 발생한다") {
                val request = mockk<HttpServletRequest>()
                every { request.cookies } returns null
                every { request.getHeader("Refresh") } returns null

                shouldThrow<TokenNotFoundException> {
                    jwtTokenExtractor.extractRefreshToken(request)
                }
            }
        }

        describe("extractId") {
            it("parseClaims를 통해 id를 반환한다") {
                val token = "sample"
                val claims = mockk<io.jsonwebtoken.Claims>()
                every { jwtProvider.parseClaims(token) } returns claims
                every { claims.get("id", Long::class.javaObjectType) } returns 77L

                val id = jwtTokenExtractor.extractId(token)

                id shouldBe 77L
                verify(exactly = 1) { jwtProvider.parseClaims(token) }
            }
        }

        describe("extractClaims") {
            it("id, email을 함께 반환한다") {
                val token = "sample"
                val claims = mockk<io.jsonwebtoken.Claims>()
                every { jwtProvider.parseClaims(token) } returns claims
                every { claims.get("id", Long::class.javaObjectType) } returns 77L
                every { claims.get("email", String::class.java) } returns "sample@com"

                val tokenClaims = jwtTokenExtractor.extractClaims(token)

                tokenClaims?.id shouldBe 77L
                tokenClaims?.email shouldBe "sample@com"
                verify(exactly = 1) { jwtProvider.parseClaims(token) }
            }
        }
    })
