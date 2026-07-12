package com.weeth.domain.user.presentation

import com.weeth.domain.user.application.dto.request.CreateMultiProfileRequest
import com.weeth.domain.user.application.dto.request.UpdateMultiProfileRequest
import com.weeth.domain.user.application.dto.response.UserProfileResponse
import com.weeth.domain.user.application.dto.response.UserProfilesResponse
import com.weeth.domain.user.application.usecase.command.AgreeTermsUseCase
import com.weeth.domain.user.application.usecase.command.AuthUserUseCase
import com.weeth.domain.user.application.usecase.command.CreateInquiryUseCase
import com.weeth.domain.user.application.usecase.command.LeaveUserUseCase
import com.weeth.domain.user.application.usecase.command.ManageUserProfileUseCase
import com.weeth.domain.user.application.usecase.command.SocialLoginUseCase
import com.weeth.domain.user.application.usecase.command.UpdateUserProfileUseCase
import com.weeth.domain.user.application.usecase.query.GetUserProfileQueryService
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
        val leaveUserUseCase = mockk<LeaveUserUseCase>()
        val manageUserProfileUseCase = mockk<ManageUserProfileUseCase>()
        val getUserProfileQueryService = mockk<GetUserProfileQueryService>()
        val tokenCookieProvider = mockk<TokenCookieProvider>()
        val controller =
            UserController(
                authUserUseCase = authUserUseCase,
                socialLoginUseCase = socialLoginUseCase,
                updateUserProfileUseCase = updateUserProfileUseCase,
                agreeTermsUseCase = agreeTermsUseCase,
                createInquiryUseCase = createInquiryUseCase,
                leaveUserUseCase = leaveUserUseCase,
                manageUserProfileUseCase = manageUserProfileUseCase,
                getUserProfileQueryService = getUserProfileQueryService,
                tokenCookieProvider = tokenCookieProvider,
            )

        beforeTest {
            clearMocks(
                authUserUseCase,
                socialLoginUseCase,
                updateUserProfileUseCase,
                agreeTermsUseCase,
                createInquiryUseCase,
                leaveUserUseCase,
                manageUserProfileUseCase,
                getUserProfileQueryService,
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
                justRun { leaveUserUseCase.execute(1L) }
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
                verify(exactly = 1) { leaveUserUseCase.execute(1L) }
            }
        }

        describe("createUserProfile") {
            it("멀티프로필을 생성한다") {
                val request = CreateMultiProfileRequest(name = "길동")
                val profileResponse = UserProfileResponse(profileId = 10L, name = "길동")
                io.mockk.every { manageUserProfileUseCase.create(1L, request) } returns profileResponse

                val response = controller.createUserProfile(request, 1L)

                response.code shouldBe UserResponseCode.USER_PROFILE_CREATED_SUCCESS.code
                response.message shouldBe UserResponseCode.USER_PROFILE_CREATED_SUCCESS.message
                response.data shouldBe profileResponse
                verify(exactly = 1) { manageUserProfileUseCase.create(1L, request) }
            }
        }

        describe("getUserProfiles") {
            it("로그인 사용자의 멀티프로필 목록을 조회한다") {
                val profilesResponse =
                    UserProfilesResponse(
                        profiles = listOf(UserProfileResponse(profileId = 10L, name = "길동")),
                    )
                io.mockk.every { getUserProfileQueryService.findAll(1L) } returns profilesResponse

                val response = controller.getUserProfiles(1L)

                response.code shouldBe UserResponseCode.USER_PROFILE_FIND_ALL_SUCCESS.code
                response.message shouldBe UserResponseCode.USER_PROFILE_FIND_ALL_SUCCESS.message
                response.data shouldBe profilesResponse
            }
        }

        describe("getUserProfile") {
            it("로그인 사용자의 멀티프로필을 단건 조회한다") {
                val profileResponse = UserProfileResponse(profileId = 10L, name = "길동")
                io.mockk.every { getUserProfileQueryService.find(1L, 10L) } returns profileResponse

                val response = controller.getUserProfile(10L, 1L)

                response.code shouldBe UserResponseCode.USER_PROFILE_FIND_SUCCESS.code
                response.message shouldBe UserResponseCode.USER_PROFILE_FIND_SUCCESS.message
                response.data shouldBe profileResponse
            }
        }

        describe("updateUserProfile") {
            it("로그인 사용자의 멀티프로필을 수정한다") {
                val request = UpdateMultiProfileRequest(name = "새 이름")
                val profileResponse = UserProfileResponse(profileId = 10L, name = "새 이름")
                io.mockk.every { manageUserProfileUseCase.update(1L, 10L, request) } returns profileResponse

                val response = controller.updateUserProfile(10L, request, 1L)

                response.code shouldBe UserResponseCode.USER_PROFILE_UPDATED_SUCCESS.code
                response.message shouldBe UserResponseCode.USER_PROFILE_UPDATED_SUCCESS.message
                response.data shouldBe profileResponse
                verify(exactly = 1) { manageUserProfileUseCase.update(1L, 10L, request) }
            }
        }
    })
