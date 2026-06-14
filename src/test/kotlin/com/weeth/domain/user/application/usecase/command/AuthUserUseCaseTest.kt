package com.weeth.domain.user.application.usecase.command

import com.weeth.global.auth.jwt.application.dto.JwtDto
import com.weeth.global.auth.jwt.application.service.JwtTokenExtractor
import com.weeth.global.auth.jwt.application.usecase.JwtManageUseCase
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.servlet.http.HttpServletRequest

class AuthUserUseCaseTest :
    DescribeSpec({
        val jwtManageUseCase = mockk<JwtManageUseCase>()
        val jwtTokenExtractor = mockk<JwtTokenExtractor>()

        val useCase =
            AuthUserUseCase(
                jwtManageUseCase = jwtManageUseCase,
                jwtTokenExtractor = jwtTokenExtractor,
            )

        beforeTest {
            clearMocks(jwtManageUseCase, jwtTokenExtractor)
        }

        describe("refreshToken") {
            it("요청에서 refresh token을 추출해 재발급한다") {
                val servletRequest = mockk<HttpServletRequest>()
                every { jwtTokenExtractor.extractRefreshToken(servletRequest) } returns "refresh-token"
                every { jwtManageUseCase.reIssueToken("refresh-token") } returns JwtDto("new-access", "new-refresh")

                val result = useCase.refreshToken(servletRequest)

                result.accessToken shouldBe "new-access"
                result.refreshToken shouldBe "new-refresh"
                verify(exactly = 1) { jwtManageUseCase.reIssueToken("refresh-token") }
            }
        }
    })
