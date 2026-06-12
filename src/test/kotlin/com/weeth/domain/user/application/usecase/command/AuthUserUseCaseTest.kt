package com.weeth.domain.user.application.usecase.command

import com.weeth.domain.user.application.exception.UserInActiveException
import com.weeth.domain.user.domain.enums.Status
import com.weeth.domain.user.domain.repository.UserReader
import com.weeth.domain.user.fixture.UserTestFixture
import com.weeth.global.auth.jwt.application.dto.JwtDto
import com.weeth.global.auth.jwt.application.service.JwtTokenExtractor
import com.weeth.global.auth.jwt.application.usecase.JwtManageUseCase
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.servlet.http.HttpServletRequest
import java.time.LocalDateTime

class AuthUserUseCaseTest :
    DescribeSpec({
        val userReader = mockk<UserReader>()
        val jwtManageUseCase = mockk<JwtManageUseCase>()
        val jwtTokenExtractor = mockk<JwtTokenExtractor>()

        val useCase =
            AuthUserUseCase(
                userReader,
                jwtManageUseCase,
                jwtTokenExtractor,
            )

        beforeTest {
            clearMocks(userReader, jwtManageUseCase, jwtTokenExtractor)
        }

        describe("leave") {
            it("회원 탈퇴 시 상태를 LEFT로 변경한다") {
                val user = UserTestFixture.createActiveUser1(1L)
                every { userReader.getById(1L) } returns user

                useCase.leave(1L)

                user.status shouldBe Status.LEFT
            }
        }

        describe("refreshToken") {
            it("요청에서 refresh token을 추출해 재발급한다") {
                val servletRequest = mockk<HttpServletRequest>()
                every { jwtTokenExtractor.extractRefreshToken(servletRequest) } returns "refresh-token"
                every { jwtTokenExtractor.extractId("refresh-token") } returns 1L
                every { userReader.getById(1L) } returns UserTestFixture.createActiveUser1(1L)
                every { jwtManageUseCase.reIssueToken("refresh-token") } returns JwtDto("new-access", "new-refresh")

                val result = useCase.refreshToken(servletRequest)

                result.accessToken shouldBe "new-access"
                result.refreshToken shouldBe "new-refresh"
            }

            it("LEFT 사용자의 refresh token이면 재발급하지 않는다") {
                val servletRequest = mockk<HttpServletRequest>()
                val user =
                    UserTestFixture
                        .createActiveUser1(1L)
                        .apply { leave(LocalDateTime.of(2026, 6, 12, 12, 0)) }
                every { jwtTokenExtractor.extractRefreshToken(servletRequest) } returns "refresh-token"
                every { jwtTokenExtractor.extractId("refresh-token") } returns 1L
                every { userReader.getById(1L) } returns user

                shouldThrow<UserInActiveException> {
                    useCase.refreshToken(servletRequest)
                }

                verify(exactly = 0) { jwtManageUseCase.reIssueToken(any()) }
            }
        }
    })
