package com.weeth.domain.user.presentation

import com.weeth.domain.user.application.usecase.command.AgreeTermsUseCase
import com.weeth.domain.user.application.usecase.command.AuthUserUseCase
import com.weeth.domain.user.application.usecase.command.CreateInquiryUseCase
import com.weeth.domain.user.application.usecase.command.SocialLoginUseCase
import com.weeth.domain.user.application.usecase.command.UpdateUserProfileUseCase
import com.weeth.domain.user.application.usecase.command.WithdrawUserUseCase
import com.weeth.global.auth.jwt.application.service.TokenCookieProvider
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.clearMocks
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie

class UserControllerTest :
    DescribeSpec({
        val authUserUseCase = mockk<AuthUserUseCase>(relaxed = true)
        val socialLoginUseCase = mockk<SocialLoginUseCase>(relaxed = true)
        val updateUserProfileUseCase = mockk<UpdateUserProfileUseCase>(relaxed = true)
        val agreeTermsUseCase = mockk<AgreeTermsUseCase>(relaxed = true)
        val createInquiryUseCase = mockk<CreateInquiryUseCase>(relaxed = true)
        val withdrawUserUseCase = mockk<WithdrawUserUseCase>()
        val tokenCookieProvider = mockk<TokenCookieProvider>()
        val controller =
            UserController(
                authUserUseCase = authUserUseCase,
                socialLoginUseCase = socialLoginUseCase,
                updateUserProfileUseCase = updateUserProfileUseCase,
                agreeTermsUseCase = agreeTermsUseCase,
                createInquiryUseCase = createInquiryUseCase,
                withdrawUserUseCase = withdrawUserUseCase,
                tokenCookieProvider = tokenCookieProvider,
            )

        beforeTest {
            clearMocks(
                authUserUseCase,
                socialLoginUseCase,
                updateUserProfileUseCase,
                agreeTermsUseCase,
                createInquiryUseCase,
                withdrawUserUseCase,
                tokenCookieProvider,
            )
        }

        fun everyExpireCookies() {
            io.mockk.every { tokenCookieProvider.expireAccessTokenCookie() } returns
                ResponseCookie
                    .from("access_token", "")
                    .path("/")
                    .maxAge(0)
                    .build()
            io.mockk.every { tokenCookieProvider.expireRefreshTokenCookie() } returns
                ResponseCookie
                    .from("refresh_token", "")
                    .path("/api/v4/users/social/refresh")
                    .maxAge(0)
                    .build()
        }

        describe("leave") {
            it("위드 탈퇴를 수행하고 access/refresh token 쿠키를 만료한다") {
                justRun { withdrawUserUseCase.execute(1L) }
                everyExpireCookies()

                val response = controller.leave(1L)

                response.statusCode.is2xxSuccessful shouldBe true
                response.body?.code shouldBe UserResponseCode.USER_LEFT_SUCCESS.code
                response.body?.message shouldBe UserResponseCode.USER_LEFT_SUCCESS.message
                val cookies = response.headers[HttpHeaders.SET_COOKIE].orEmpty()
                cookies shouldHaveSize 2
                cookies[0] shouldContain "access_token="
                cookies[0] shouldContain "Max-Age=0"
                cookies[1] shouldContain "refresh_token="
                cookies[1] shouldContain "Max-Age=0"
                verify(exactly = 1) { withdrawUserUseCase.execute(1L) }
            }
        }
    })
