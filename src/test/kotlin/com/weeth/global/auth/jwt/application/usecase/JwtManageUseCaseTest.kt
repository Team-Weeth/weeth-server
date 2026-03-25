package com.weeth.global.auth.jwt.application.usecase

import com.weeth.global.auth.jwt.application.dto.JwtDto
import com.weeth.global.auth.jwt.application.service.JwtTokenExtractor
import com.weeth.global.auth.jwt.domain.enums.TokenType
import com.weeth.global.auth.jwt.domain.port.RefreshTokenStorePort
import com.weeth.global.auth.jwt.domain.service.JwtTokenProvider
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify

class JwtManageUseCaseTest :
    DescribeSpec({
        val jwtProvider = mockk<JwtTokenProvider>()
        val jwtService = mockk<JwtTokenExtractor>()
        val refreshTokenStore = mockk<RefreshTokenStorePort>(relaxUnitFun = true)
        val useCase = JwtManageUseCase(jwtProvider, jwtService, refreshTokenStore)

        beforeTest { clearMocks(jwtProvider, jwtService, refreshTokenStore) }

        describe("create") {
            it("ACCESS 타입으로 토큰을 생성하고 저장한다") {
                every { jwtProvider.createAccessToken(1L, "a@weeth.com", TokenType.ACCESS) } returns "access"
                every { jwtProvider.createRefreshToken(1L) } returns "refresh"

                val result = useCase.create(1L, "a@weeth.com", TokenType.ACCESS)

                result shouldBe JwtDto("access", "refresh")
                verify(exactly = 1) { refreshTokenStore.save(1L, "refresh", "a@weeth.com", TokenType.ACCESS) }
            }

            it("TEMPORARY 타입으로 토큰을 생성하고 저장한다") {
                every { jwtProvider.createAccessToken(1L, "a@weeth.com", TokenType.TEMPORARY) } returns "temp-access"
                every { jwtProvider.createRefreshToken(1L) } returns "refresh"

                val result = useCase.create(1L, "a@weeth.com", TokenType.TEMPORARY)

                result shouldBe JwtDto("temp-access", "refresh")
                verify(exactly = 1) { refreshTokenStore.save(1L, "refresh", "a@weeth.com", TokenType.TEMPORARY) }
            }
        }

        describe("reIssueToken") {
            it("저장된 tokenType으로 새 토큰을 재발급한다") {
                every { jwtProvider.validate("old-refresh") } just runs
                every { jwtService.extractId("old-refresh") } returns 10L
                every { refreshTokenStore.getEmail(10L) } returns "admin@weeth.com"
                every { refreshTokenStore.getTokenType(10L) } returns TokenType.ACCESS
                every { jwtProvider.createAccessToken(10L, "admin@weeth.com", TokenType.ACCESS) } returns "new-access"
                every { jwtProvider.createRefreshToken(10L) } returns "new-refresh"

                val result = useCase.reIssueToken("old-refresh")

                result shouldBe JwtDto("new-access", "new-refresh")
                verify(exactly = 1) { refreshTokenStore.validateRefreshToken(10L, "old-refresh") }
                verify(exactly = 1) { refreshTokenStore.save(10L, "new-refresh", "admin@weeth.com", TokenType.ACCESS) }
            }

            it("TEMPORARY tokenType이면 TEMPORARY 토큰으로 재발급한다") {
                every { jwtProvider.validate("old-refresh") } just runs
                every { jwtService.extractId("old-refresh") } returns 10L
                every { refreshTokenStore.getEmail(10L) } returns "new@weeth.com"
                every { refreshTokenStore.getTokenType(10L) } returns TokenType.TEMPORARY
                every {
                    jwtProvider.createAccessToken(10L, "new@weeth.com", TokenType.TEMPORARY)
                } returns "temp-access"
                every { jwtProvider.createRefreshToken(10L) } returns "new-refresh"

                val result = useCase.reIssueToken("old-refresh")

                result shouldBe JwtDto("temp-access", "new-refresh")
                verify(exactly = 1) {
                    refreshTokenStore.save(10L, "new-refresh", "new@weeth.com", TokenType.TEMPORARY)
                }
            }
        }
    })
