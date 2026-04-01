package com.weeth.domain.user.application.usecase.command

import com.fasterxml.jackson.databind.ObjectMapper
import com.weeth.domain.file.domain.port.FileAccessUrlPort
import com.weeth.domain.user.application.dto.request.SocialLoginRequest
import com.weeth.domain.user.application.exception.EmailNotFoundException
import com.weeth.domain.user.application.mapper.UserMapper
import com.weeth.domain.user.domain.entity.User
import com.weeth.domain.user.domain.entity.UserSocialAccount
import com.weeth.domain.user.domain.enums.SocialProvider
import com.weeth.domain.user.domain.port.SocialAuthPort
import com.weeth.domain.user.domain.repository.UserRepository
import com.weeth.domain.user.domain.repository.UserSocialAccountRepository
import com.weeth.domain.user.domain.vo.SocialAuthResult
import com.weeth.domain.user.fixture.UserTestFixture
import com.weeth.domain.user.infrastructure.SocialAuthPortRegistry
import com.weeth.global.auth.jwt.application.dto.JwtDto
import com.weeth.global.auth.jwt.application.usecase.JwtManageUseCase
import com.weeth.global.auth.jwt.domain.enums.TokenType
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.Optional

class SocialLoginUseCaseTest :
    DescribeSpec({
        val userRepository = mockk<UserRepository>()
        val userSocialAccountRepository = mockk<UserSocialAccountRepository>()
        val socialAuthPortRegistry = mockk<SocialAuthPortRegistry>()
        val socialAuthPort = mockk<SocialAuthPort>()
        val jwtManageUseCase = mockk<JwtManageUseCase>()
        val fileAccessUrlPort = mockk<FileAccessUrlPort>()
        val userMapper = UserMapper(fileAccessUrlPort)
        val objectMapper = mockk<ObjectMapper>()

        val useCase =
            SocialLoginUseCase(
                userRepository = userRepository,
                userSocialAccountRepository = userSocialAccountRepository,
                socialAuthPortRegistry = socialAuthPortRegistry,
                jwtManageUseCase = jwtManageUseCase,
                userMapper = userMapper,
                objectMapper,
            )

        beforeTest {
            clearMocks(
                userRepository,
                userSocialAccountRepository,
                socialAuthPortRegistry,
                socialAuthPort,
                jwtManageUseCase,
            )
        }

        describe("socialLoginByApple") {
            it("약관 동의 완료된 기존 유저는 ACCESS 토큰과 registered=true를 반환한다") {
                val request = SocialLoginRequest(authCode = "apple-auth-code")
                val user = UserTestFixture.createRegisteredUser(1L)
                val account =
                    UserSocialAccount(
                        provider = SocialProvider.APPLE,
                        providerUserId = "apple-user-1",
                        user = user,
                    )
                val authResult =
                    SocialAuthResult(
                        provider = SocialProvider.APPLE,
                        providerUserId = "apple-user-1",
                        email = "",
                        emailVerified = false,
                        name = null,
                    )

                every { socialAuthPortRegistry.get(SocialProvider.APPLE) } returns socialAuthPort
                every { socialAuthPort.authenticate("apple-auth-code") } returns authResult
                every {
                    userSocialAccountRepository.findByProviderAndProviderUserId(SocialProvider.APPLE, "apple-user-1")
                } returns Optional.of(account)
                every { jwtManageUseCase.create(user.id, user.emailValue, TokenType.ACCESS) } returns
                    JwtDto("access", "refresh")

                val result = useCase.socialLoginByApple(request)

                result.accessToken shouldBe "access"
                result.refreshToken shouldBe "refresh"
                result.registered shouldBe true

                verify(exactly = 0) { userRepository.save(any()) }
                verify(exactly = 0) { userSocialAccountRepository.save(any()) }
            }

            it("약관 미동의 기존 유저는 TEMPORARY 토큰과 registered=false를 반환한다") {
                val request = SocialLoginRequest(authCode = "apple-auth-code")
                val user = UserTestFixture.createActiveUser1(1L) // ACTIVE이지만 약관 미동의
                val account =
                    UserSocialAccount(
                        provider = SocialProvider.APPLE,
                        providerUserId = "apple-user-1",
                        user = user,
                    )
                val authResult =
                    SocialAuthResult(
                        provider = SocialProvider.APPLE,
                        providerUserId = "apple-user-1",
                        email = "",
                        emailVerified = false,
                        name = null,
                    )

                every { socialAuthPortRegistry.get(SocialProvider.APPLE) } returns socialAuthPort
                every { socialAuthPort.authenticate("apple-auth-code") } returns authResult
                every {
                    userSocialAccountRepository.findByProviderAndProviderUserId(SocialProvider.APPLE, "apple-user-1")
                } returns Optional.of(account)
                every { jwtManageUseCase.create(user.id, user.emailValue, TokenType.TEMPORARY) } returns
                    JwtDto("temp-access", "refresh")

                val result = useCase.socialLoginByApple(request)

                result.accessToken shouldBe "temp-access"
                result.registered shouldBe false
            }

            it("신규 유저는 TEMPORARY 토큰과 registered=false를 반환한다") {
                val request = SocialLoginRequest(authCode = "apple-auth-code")
                val authResult =
                    SocialAuthResult(
                        provider = SocialProvider.APPLE,
                        providerUserId = "apple-user-new",
                        email = "new@test.com",
                        emailVerified = true,
                        name = "신규유저",
                    )

                every { socialAuthPortRegistry.get(SocialProvider.APPLE) } returns socialAuthPort
                every { socialAuthPort.authenticate("apple-auth-code") } returns authResult
                every {
                    userSocialAccountRepository.findByProviderAndProviderUserId(SocialProvider.APPLE, "apple-user-new")
                } returns Optional.empty()
                every { userRepository.save(any<User>()) } answers {
                    val saved = firstArg<User>()
                    saved
                }
                every { userSocialAccountRepository.save(any<UserSocialAccount>()) } answers { firstArg() }
                every { jwtManageUseCase.create(any(), any(), TokenType.TEMPORARY) } returns
                    JwtDto("temp-access", "refresh")

                val result = useCase.socialLoginByApple(request)

                result.registered shouldBe false
                verify(exactly = 1) { jwtManageUseCase.create(any(), any(), TokenType.TEMPORARY) }
            }

            it("신규 연동 계정은 이메일이 없으면 예외가 발생한다") {
                val request = SocialLoginRequest(authCode = "apple-auth-code")
                val authResult =
                    SocialAuthResult(
                        provider = SocialProvider.APPLE,
                        providerUserId = "apple-user-2",
                        email = "",
                        emailVerified = false,
                        name = null,
                    )

                every { socialAuthPortRegistry.get(SocialProvider.APPLE) } returns socialAuthPort
                every { socialAuthPort.authenticate("apple-auth-code") } returns authResult
                every {
                    userSocialAccountRepository.findByProviderAndProviderUserId(SocialProvider.APPLE, "apple-user-2")
                } returns Optional.empty()

                shouldThrow<EmailNotFoundException> {
                    useCase.socialLoginByApple(request)
                }

                verify(exactly = 0) { userRepository.save(any()) }
                verify(exactly = 0) { userSocialAccountRepository.save(any()) }
            }
        }
    })
