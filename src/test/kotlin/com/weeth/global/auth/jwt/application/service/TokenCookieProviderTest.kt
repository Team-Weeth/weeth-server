package com.weeth.global.auth.jwt.application.service

import com.weeth.global.config.properties.CookieProperties
import com.weeth.global.config.properties.JwtProperties
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class TokenCookieProviderTest :
    DescribeSpec({
        val jwtProperties =
            JwtProperties(
                key = "test-key",
                access = JwtProperties.TokenProperties(expiration = 3_600_000L, header = "Authorization"),
                refresh = JwtProperties.TokenProperties(expiration = 604_800_000L, header = "Authorization_refresh"),
            )

        describe("createAccessTokenCookie") {
            it("설정값대로 access token 쿠키를 생성한다") {
                val cookieProperties =
                    CookieProperties(
                        accessTokenName = "access_token",
                        refreshTokenName = "refresh_token",
                        secure = false,
                    )
                val provider = TokenCookieProvider(cookieProperties, jwtProperties)

                val cookie = provider.createAccessTokenCookie("test-access-token")

                cookie.name shouldBe "access_token"
                cookie.value shouldBe "test-access-token"
                cookie.maxAge.seconds shouldBe 3600L
                cookie.path shouldBe "/"
                cookie.isHttpOnly shouldBe true
                cookie.isSecure shouldBe false
                cookie.sameSite shouldBe "Lax"
            }

            it("domain이 설정되면 쿠키에 도메인이 포함된다") {
                val cookieProperties =
                    CookieProperties(
                        accessTokenName = "access_token",
                        refreshTokenName = "refresh_token",
                        domain = "example.com",
                    )
                val provider = TokenCookieProvider(cookieProperties, jwtProperties)

                val cookie = provider.createAccessTokenCookie("test-token")

                cookie.toString() shouldContain "Domain=example.com"
            }

            it("domain이 빈 문자열이면 쿠키에 도메인이 포함되지 않는다") {
                val cookieProperties =
                    CookieProperties(accessTokenName = "access_token", refreshTokenName = "refresh_token", domain = "")
                val provider = TokenCookieProvider(cookieProperties, jwtProperties)

                val cookie = provider.createAccessTokenCookie("test-token")

                cookie.toString().contains("Domain=") shouldBe false
            }
        }

        describe("createRefreshTokenCookie") {
            it("설정값대로 refresh token 쿠키를 생성한다") {
                val cookieProperties =
                    CookieProperties(
                        accessTokenName = "access_token",
                        refreshTokenName = "refresh_token",
                        secure = true,
                        sameSite = "None",
                    )
                val provider = TokenCookieProvider(cookieProperties, jwtProperties)

                val cookie = provider.createRefreshTokenCookie("test-refresh-token")

                cookie.name shouldBe "refresh_token"
                cookie.value shouldBe "test-refresh-token"
                cookie.maxAge.seconds shouldBe 604_800L
                cookie.path shouldBe "/api/v4/users/social/refresh"
                cookie.isHttpOnly shouldBe true
                cookie.isSecure shouldBe true
                cookie.sameSite shouldBe "None"
            }
        }

        describe("expireAccessTokenCookie") {
            it("access token 쿠키를 같은 이름과 path로 만료한다") {
                val cookieProperties =
                    CookieProperties(
                        accessTokenName = "access_token",
                        refreshTokenName = "refresh_token",
                        path = "/",
                    )
                val provider = TokenCookieProvider(cookieProperties, jwtProperties)

                val cookie = provider.expireAccessTokenCookie()

                cookie.name shouldBe "access_token"
                cookie.value shouldBe ""
                cookie.maxAge.seconds shouldBe 0L
                cookie.path shouldBe "/"
            }
        }

        describe("expireRefreshTokenCookie") {
            it("refresh token 쿠키를 같은 이름과 refresh path로 만료한다") {
                val cookieProperties =
                    CookieProperties(
                        accessTokenName = "access_token",
                        refreshTokenName = "refresh_token",
                        refreshPath = "/api/v4/users/social/refresh",
                    )
                val provider = TokenCookieProvider(cookieProperties, jwtProperties)

                val cookie = provider.expireRefreshTokenCookie()

                cookie.name shouldBe "refresh_token"
                cookie.value shouldBe ""
                cookie.maxAge.seconds shouldBe 0L
                cookie.path shouldBe "/api/v4/users/social/refresh"
            }
        }
    })
