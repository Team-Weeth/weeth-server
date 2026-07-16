package com.weeth.domain.user.presentation

import com.weeth.domain.attendance.domain.enums.AttendanceStatus
import com.weeth.domain.user.application.dto.request.AssignClubProfileRequest
import com.weeth.domain.user.application.dto.request.ClubProfileAssignmentRequest
import com.weeth.domain.user.application.dto.request.CreateMultiProfileRequest
import com.weeth.domain.user.application.dto.request.UpdateMultiProfileRequest
import com.weeth.domain.user.application.dto.response.UserAttendedSessionResponse
import com.weeth.domain.user.application.dto.response.UserMyPageInfoResponse
import com.weeth.domain.user.application.dto.response.UserMyPageResponse
import com.weeth.domain.user.application.dto.response.UserMyPageStatsResponse
import com.weeth.domain.user.application.dto.response.UserMyPostResponse
import com.weeth.domain.user.application.dto.response.UserProfileResponse
import com.weeth.domain.user.application.dto.response.UserProfilesResponse
import com.weeth.domain.user.application.usecase.command.AgreeTermsUseCase
import com.weeth.domain.user.application.usecase.command.AuthUserUseCase
import com.weeth.domain.user.application.usecase.command.CreateInquiryUseCase
import com.weeth.domain.user.application.usecase.command.LeaveUserUseCase
import com.weeth.domain.user.application.usecase.command.ManageUserProfileUseCase
import com.weeth.domain.user.application.usecase.command.SocialLoginUseCase
import com.weeth.domain.user.application.usecase.command.UpdateUserProfileUseCase
import com.weeth.domain.user.application.usecase.query.GetUserAttendanceQueryService
import com.weeth.domain.user.application.usecase.query.GetUserMyPageQueryService
import com.weeth.domain.user.application.usecase.query.GetUserPostQueryService
import com.weeth.domain.user.application.usecase.query.GetUserProfileQueryService
import com.weeth.global.auth.jwt.application.service.TokenCookieProvider
import com.weeth.global.common.response.SliceResponse
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
import java.time.LocalDateTime

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
        val getUserMyPageQueryService = mockk<GetUserMyPageQueryService>()
        val getUserPostQueryService = mockk<GetUserPostQueryService>()
        val getUserAttendanceQueryService = mockk<GetUserAttendanceQueryService>()
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
                getUserMyPageQueryService = getUserMyPageQueryService,
                getUserPostQueryService = getUserPostQueryService,
                getUserAttendanceQueryService = getUserAttendanceQueryService,
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
                getUserMyPageQueryService,
                getUserPostQueryService,
                getUserAttendanceQueryService,
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
                val request = CreateMultiProfileRequest(name = "길동", clubIds = listOf("1A2b3C"))
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

        describe("getMyPage") {
            it("마이페이지 요약 정보를 조회한다") {
                val myPageResponse =
                    UserMyPageResponse(
                        user =
                            UserMyPageInfoResponse(
                                name = "홍길동",
                                tel = "01012345678",
                                email = "hong@example.com",
                                school = "가천대학교",
                                department = "컴퓨터공학과",
                                studentId = "20201234",
                            ),
                        stats = UserMyPageStatsResponse(postCount = 12L, attendedSessionCount = 8L),
                        usingProfiles = emptyList(),
                    )
                io.mockk.every { getUserMyPageQueryService.getMyPage(1L) } returns myPageResponse

                val response = controller.getMyPage(1L)

                response.code shouldBe UserResponseCode.USER_MY_PAGE_FIND_SUCCESS.code
                response.message shouldBe UserResponseCode.USER_MY_PAGE_FIND_SUCCESS.message
                response.data shouldBe myPageResponse
                verify(exactly = 1) { getUserMyPageQueryService.getMyPage(1L) }
            }
        }

        describe("getMyPosts") {
            it("로그인 사용자가 작성한 게시글 목록을 조회한다") {
                val postsResponse =
                    SliceResponse(
                        content =
                            listOf(
                                UserMyPostResponse(
                                    postId = 200L,
                                    clubId = "1C",
                                    clubName = "Leets",
                                    boardId = 10L,
                                    boardName = "자유게시판",
                                    title = "제목",
                                    content = "내용",
                                    commentCount = 3,
                                    likeCount = 5,
                                    createdAt = LocalDateTime.of(2026, 6, 29, 10, 0),
                                ),
                            ),
                        pageNumber = 0,
                        pageSize = 5,
                        numberOfElements = 1,
                        hasNext = true,
                    )
                io.mockk.every { getUserPostQueryService.getMyPosts(1L, 0, 5) } returns postsResponse

                val response = controller.getMyPosts(userId = 1L, pageNumber = 0, pageSize = 5)

                response.code shouldBe UserResponseCode.USER_MY_POSTS_FIND_SUCCESS.code
                response.message shouldBe UserResponseCode.USER_MY_POSTS_FIND_SUCCESS.message
                response.data shouldBe postsResponse
                verify(exactly = 1) { getUserPostQueryService.getMyPosts(1L, 0, 5) }
            }
        }

        describe("getAttendedSessions") {
            it("로그인 사용자가 출석한 세션 목록을 조회한다") {
                val attendedSessionsResponse =
                    SliceResponse(
                        content =
                            listOf(
                                UserAttendedSessionResponse(
                                    attendanceId = 1L,
                                    clubId = "1C",
                                    clubName = "Leets",
                                    sessionId = 10L,
                                    sessionTitle = "1차 정기모임",
                                    cardinal = 6,
                                    start = LocalDateTime.of(2026, 6, 29, 19, 0),
                                    end = LocalDateTime.of(2026, 6, 29, 21, 0),
                                    status = AttendanceStatus.ATTEND,
                                ),
                            ),
                        pageNumber = 0,
                        pageSize = 5,
                        numberOfElements = 1,
                        hasNext = false,
                    )
                io.mockk.every {
                    getUserAttendanceQueryService.getAttendedSessions(1L, 0, 5)
                } returns attendedSessionsResponse

                val response = controller.getAttendedSessions(userId = 1L, pageNumber = 0, pageSize = 5)

                response.code shouldBe UserResponseCode.USER_ATTENDED_SESSIONS_FIND_SUCCESS.code
                response.message shouldBe UserResponseCode.USER_ATTENDED_SESSIONS_FIND_SUCCESS.message
                response.data shouldBe attendedSessionsResponse
                verify(exactly = 1) { getUserAttendanceQueryService.getAttendedSessions(1L, 0, 5) }
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

        describe("assignClubProfiles") {
            it("동아리별 사용 프로필을 변경한다") {
                val request =
                    AssignClubProfileRequest(
                        assignments =
                            listOf(
                                ClubProfileAssignmentRequest(clubId = "1C", profileId = 10L),
                            ),
                    )
                justRun { manageUserProfileUseCase.assignClubProfiles(1L, request) }

                val response = controller.assignClubProfiles(request, 1L)

                response.code shouldBe UserResponseCode.USER_PROFILE_ASSIGNMENT_UPDATED_SUCCESS.code
                response.message shouldBe UserResponseCode.USER_PROFILE_ASSIGNMENT_UPDATED_SUCCESS.message
                verify(exactly = 1) { manageUserProfileUseCase.assignClubProfiles(1L, request) }
            }
        }

        describe("deleteUserProfile") {
            it("로그인 사용자의 멀티프로필을 삭제한다") {
                justRun { manageUserProfileUseCase.delete(1L, 10L) }

                val response = controller.deleteUserProfile(10L, 1L)

                response.code shouldBe UserResponseCode.USER_PROFILE_DELETED_SUCCESS.code
                response.message shouldBe UserResponseCode.USER_PROFILE_DELETED_SUCCESS.message
                verify(exactly = 1) { manageUserProfileUseCase.delete(1L, 10L) }
            }
        }

        describe("deleteUserProfileImage") {
            it("로그인 사용자의 멀티프로필 프로필 사진을 삭제한다") {
                justRun { manageUserProfileUseCase.deleteProfileImage(userId = 1L, profileId = 10L) }

                val response = controller.deleteUserProfileImage(profileId = 10L, userId = 1L)

                response.code shouldBe UserResponseCode.USER_PROFILE_IMAGE_DELETED_SUCCESS.code
                response.message shouldBe UserResponseCode.USER_PROFILE_IMAGE_DELETED_SUCCESS.message
                verify(exactly = 1) { manageUserProfileUseCase.deleteProfileImage(userId = 1L, profileId = 10L) }
            }
        }

        describe("deleteUserProfileHeaderImage") {
            it("로그인 사용자의 멀티프로필 헤더 사진을 삭제한다") {
                justRun { manageUserProfileUseCase.deleteHeaderImage(userId = 1L, profileId = 10L) }

                val response = controller.deleteUserProfileHeaderImage(profileId = 10L, userId = 1L)

                response.code shouldBe UserResponseCode.USER_PROFILE_HEADER_IMAGE_DELETED_SUCCESS.code
                response.message shouldBe UserResponseCode.USER_PROFILE_HEADER_IMAGE_DELETED_SUCCESS.message
                verify(exactly = 1) { manageUserProfileUseCase.deleteHeaderImage(userId = 1L, profileId = 10L) }
            }
        }
    })
