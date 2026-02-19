package com.weeth.global.auth.jwt.application.usecase

import com.weeth.domain.user.domain.entity.enums.Role
import com.weeth.global.auth.jwt.application.dto.JwtDto
import com.weeth.global.auth.jwt.application.service.JwtTokenExtractor
import com.weeth.global.auth.jwt.domain.port.RefreshTokenStorePort
import com.weeth.global.auth.jwt.domain.service.JwtTokenProvider
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
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

        describe("create") {
            it("access/refresh token을 생성하고 저장한다") {
                every { jwtProvider.createAccessToken(1L, "a@weeth.com", Role.USER) } returns "access"
                every { jwtProvider.createRefreshToken(1L) } returns "refresh"

                val result = useCase.create(1L, "a@weeth.com", Role.USER)

                result shouldBe JwtDto("access", "refresh")
                verify(exactly = 1) { refreshTokenStore.save(1L, "refresh", Role.USER, "a@weeth.com") }
            }
        }

        describe("reIssueToken") {
            it("저장 토큰 검증 후 새 토큰을 재발급한다") {
                every { jwtProvider.validate("old-refresh") } just runs
                every { jwtService.extractId("old-refresh") } returns 10L
                every { refreshTokenStore.getRole(10L) } returns Role.ADMIN
                every { refreshTokenStore.getEmail(10L) } returns "admin@weeth.com"
                every { jwtProvider.createAccessToken(10L, "admin@weeth.com", Role.ADMIN) } returns "new-access"
                every { jwtProvider.createRefreshToken(10L) } returns "new-refresh"

                val result = useCase.reIssueToken("old-refresh")

                result shouldBe JwtDto("new-access", "new-refresh")
                verify(exactly = 1) { refreshTokenStore.validateRefreshToken(10L, "old-refresh") }
                verify(exactly = 1) { refreshTokenStore.save(10L, "new-refresh", Role.ADMIN, "admin@weeth.com") }
            }
        }
    })
