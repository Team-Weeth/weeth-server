package com.weeth.domain.user.presentation

import com.weeth.domain.user.application.dto.request.AgreeTermsRequest
import com.weeth.domain.user.application.dto.request.AssignClubProfileRequest
import com.weeth.domain.user.application.dto.request.CreateInquiryRequest
import com.weeth.domain.user.application.dto.request.CreateMultiProfileRequest
import com.weeth.domain.user.application.dto.request.SocialLoginRequest
import com.weeth.domain.user.application.dto.request.UpdateMultiProfileRequest
import com.weeth.domain.user.application.dto.request.UpdateUserProfileRequest
import com.weeth.domain.user.application.dto.response.SocialLoginResponse
import com.weeth.domain.user.application.dto.response.UserMyPageResponse
import com.weeth.domain.user.application.dto.response.UserProfileResponse
import com.weeth.domain.user.application.dto.response.UserProfilesResponse
import com.weeth.domain.user.application.exception.UserErrorCode
import com.weeth.domain.user.application.usecase.command.AgreeTermsUseCase
import com.weeth.domain.user.application.usecase.command.AuthUserUseCase
import com.weeth.domain.user.application.usecase.command.CreateInquiryUseCase
import com.weeth.domain.user.application.usecase.command.LeaveUserUseCase
import com.weeth.domain.user.application.usecase.command.ManageUserProfileUseCase
import com.weeth.domain.user.application.usecase.command.SocialLoginUseCase
import com.weeth.domain.user.application.usecase.command.UpdateUserProfileUseCase
import com.weeth.domain.user.application.usecase.query.GetUserMyPageQueryService
import com.weeth.domain.user.application.usecase.query.GetUserProfileQueryService
import com.weeth.global.auth.annotation.CurrentUser
import com.weeth.global.auth.jwt.application.dto.JwtDto
import com.weeth.global.auth.jwt.application.exception.JwtErrorCode
import com.weeth.global.auth.jwt.application.service.TokenCookieProvider
import com.weeth.global.common.exception.ApiErrorCodeExample
import com.weeth.global.common.response.CommonResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "USER", description = "사용자 API")
@RestController
@RequestMapping("/api/v4/users")
@ApiErrorCodeExample(UserErrorCode::class, JwtErrorCode::class)
class UserController(
    private val authUserUseCase: AuthUserUseCase,
    private val socialLoginUseCase: SocialLoginUseCase,
    private val updateUserProfileUseCase: UpdateUserProfileUseCase,
    private val agreeTermsUseCase: AgreeTermsUseCase,
    private val createInquiryUseCase: CreateInquiryUseCase,
    private val leaveUserUseCase: LeaveUserUseCase,
    private val manageUserProfileUseCase: ManageUserProfileUseCase,
    private val getUserProfileQueryService: GetUserProfileQueryService,
    private val getUserMyPageQueryService: GetUserMyPageQueryService,
    private val tokenCookieProvider: TokenCookieProvider,
) {
    @PostMapping("/social/kakao")
    @Operation(summary = "카카오 소셜 로그인(auth code flow)")
    @SecurityRequirements
    fun socialLoginByKakao(
        @RequestBody @Valid request: SocialLoginRequest,
    ): ResponseEntity<CommonResponse<SocialLoginResponse>> {
        val response = socialLoginUseCase.socialLoginByKakao(request)
        return buildTokenResponse(
            CommonResponse.success(UserResponseCode.SOCIAL_LOGIN_SUCCESS, response),
            response.accessToken,
            response.refreshToken,
        )
    }

    @PostMapping("/social/apple")
    @Operation(summary = "애플 소셜 로그인(auth code flow)")
    @SecurityRequirements
    fun socialLoginByApple(
        @RequestBody @Valid request: SocialLoginRequest,
    ): ResponseEntity<CommonResponse<SocialLoginResponse>> {
        val response = socialLoginUseCase.socialLoginByApple(request)
        return buildTokenResponse(
            CommonResponse.success(UserResponseCode.SOCIAL_LOGIN_SUCCESS, response),
            response.accessToken,
            response.refreshToken,
        )
    }

    @PostMapping("/social/refresh")
    @Operation(summary = "토큰 재발급", description = "쿠키를 사용해 토큰을 재발급합니다.")
    @SecurityRequirements
    fun refreshToken(request: HttpServletRequest): ResponseEntity<CommonResponse<JwtDto>> {
        val jwtDto = authUserUseCase.refreshToken(request)
        return buildTokenResponse(
            CommonResponse.success(UserResponseCode.JWT_REFRESH_SUCCESS, jwtDto),
            jwtDto.accessToken,
            jwtDto.refreshToken,
        )
    }

    @PostMapping("/terms")
    @Operation(summary = "약관 동의")
    fun agreeTerms(
        @RequestBody @Valid request: AgreeTermsRequest,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): ResponseEntity<CommonResponse<JwtDto>> {
        val jwtDto = agreeTermsUseCase.execute(userId, request)
        return buildTokenResponse(
            CommonResponse.success(UserResponseCode.USER_TERMS_AGREE_SUCCESS, jwtDto),
            jwtDto.accessToken,
            jwtDto.refreshToken,
        )
    }

    @PatchMapping
    @Operation(summary = "내 정보 수정")
    fun update(
        @RequestBody @Valid request: UpdateUserProfileRequest,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<Void> {
        updateUserProfileUseCase.updateProfile(request, userId)
        return CommonResponse.success(UserResponseCode.USER_UPDATE_SUCCESS)
    }

    @DeleteMapping("/me")
    @Operation(summary = "위드 탈퇴")
    fun leave(
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): ResponseEntity<CommonResponse<Void>> {
        leaveUserUseCase.execute(userId)
        return buildExpiredTokenResponse(CommonResponse.success(UserResponseCode.USER_LEFT_SUCCESS))
    }

    @PostMapping("/inquiries")
    @Operation(summary = "문의하기")
    @SecurityRequirements
    fun createInquiry(
        @RequestBody @Valid request: CreateInquiryRequest,
    ): CommonResponse<Void> {
        createInquiryUseCase.execute(request)
        return CommonResponse.success(UserResponseCode.INQUIRY_SEND_SUCCESS)
    }

    @PostMapping("/me/profiles")
    @Operation(summary = "멀티프로필 생성")
    fun createUserProfile(
        @RequestBody @Valid request: CreateMultiProfileRequest,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<UserProfileResponse> {
        val response = manageUserProfileUseCase.create(userId, request)
        return CommonResponse.success(UserResponseCode.USER_PROFILE_CREATED_SUCCESS, response)
    }

    @GetMapping("/me/profiles")
    @Operation(summary = "멀티프로필 목록 조회")
    fun getUserProfiles(
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<UserProfilesResponse> {
        val response = getUserProfileQueryService.findAll(userId)
        return CommonResponse.success(UserResponseCode.USER_PROFILE_FIND_ALL_SUCCESS, response)
    }

    @GetMapping("/me/profiles/{profileId}")
    @Operation(summary = "멀티프로필 단건 조회")
    fun getUserProfile(
        @PathVariable profileId: Long,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<UserProfileResponse> {
        val response = getUserProfileQueryService.find(userId, profileId)
        return CommonResponse.success(UserResponseCode.USER_PROFILE_FIND_SUCCESS, response)
    }

    @PatchMapping("/me/profiles/{profileId}")
    @Operation(summary = "멀티프로필 수정")
    fun updateUserProfile(
        @PathVariable profileId: Long,
        @RequestBody @Valid request: UpdateMultiProfileRequest,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<UserProfileResponse> {
        val response = manageUserProfileUseCase.update(userId, profileId, request)
        return CommonResponse.success(UserResponseCode.USER_PROFILE_UPDATED_SUCCESS, response)
    }

    @DeleteMapping("/me/profiles/{profileId}")
    @Operation(summary = "멀티프로필 삭제")
    fun deleteUserProfile(
        @PathVariable profileId: Long,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<Void> {
        manageUserProfileUseCase.delete(userId, profileId)
        return CommonResponse.success(UserResponseCode.USER_PROFILE_DELETED_SUCCESS)
    }

    @PatchMapping("/me/club-profile-assignments")
    @Operation(summary = "동아리별 사용 프로필 변경")
    fun assignClubProfiles(
        @RequestBody @Valid request: AssignClubProfileRequest,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<Void> {
        manageUserProfileUseCase.assignClubProfiles(userId, request)
        return CommonResponse.success(UserResponseCode.USER_PROFILE_ASSIGNMENT_UPDATED_SUCCESS)
    }

    @GetMapping("/me/mypage")
    @Operation(summary = "마이페이지 요약 조회")
    fun getMyPage(
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<UserMyPageResponse> {
        val response = getUserMyPageQueryService.getMyPage(userId)
        return CommonResponse.success(UserResponseCode.USER_MY_PAGE_FIND_SUCCESS, response)
    }

    private fun <T> buildTokenResponse(
        body: CommonResponse<T>,
        accessToken: String,
        refreshToken: String,
    ): ResponseEntity<CommonResponse<T>> =
        ResponseEntity
            .ok()
            .header(HttpHeaders.SET_COOKIE, tokenCookieProvider.createAccessTokenCookie(accessToken).toString())
            .header(HttpHeaders.SET_COOKIE, tokenCookieProvider.createRefreshTokenCookie(refreshToken).toString())
            .body(body)

    private fun buildExpiredTokenResponse(body: CommonResponse<Void>): ResponseEntity<CommonResponse<Void>> =
        ResponseEntity
            .ok()
            .header(HttpHeaders.SET_COOKIE, tokenCookieProvider.expireAccessTokenCookie().toString())
            .header(HttpHeaders.SET_COOKIE, tokenCookieProvider.expireRefreshTokenCookie().toString())
            .body(body)
}
