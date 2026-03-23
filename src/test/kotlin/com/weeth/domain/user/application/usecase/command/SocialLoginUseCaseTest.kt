package com.weeth.domain.user.application.usecase.command

import com.weeth.domain.file.domain.port.FileAccessUrlPort
import com.weeth.domain.user.application.dto.request.SocialLoginRequest
import com.weeth.domain.user.application.exception.EmailNotFoundException
import com.weeth.domain.user.application.mapper.UserMapper
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
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
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

        val useCase =
            SocialLoginUseCase(
                userRepository = userRepository,
                userSocialAccountRepository = userSocialAccountRepository,
                socialAuthPortRegistry = socialAuthPortRegistry,
                jwtManageUseCase = jwtManageUseCase,
                userMapper = userMapper,
            )

        describe("socialLoginByApple") {
            it("기존 연동 계정은 이메일이 없어도 로그인된다") {
                val request = SocialLoginRequest(authCode = "apple-auth-code")
                val user = UserTestFixture.createActiveUser1(1L)
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
                every { jwtManageUseCase.create(user.id, user.emailValue) } returns
                    JwtDto("access", "refresh")

                val result = useCase.socialLoginByApple(request)

                result.accessToken shouldBe "access"
                result.refreshToken shouldBe "refresh"
                result.isNewUser shouldBe false

                verify(exactly = 0) { userRepository.save(any()) }
                verify(exactly = 0) { userSocialAccountRepository.save(any()) }
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
