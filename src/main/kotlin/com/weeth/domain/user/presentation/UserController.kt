package com.weeth.domain.user.presentation

import com.weeth.domain.user.application.dto.request.SignUpRequest
import com.weeth.domain.user.application.dto.request.SocialLoginRequest
import com.weeth.domain.user.application.dto.request.UpdateUserProfileRequest
import com.weeth.domain.user.application.dto.response.SocialLoginResponse
import com.weeth.domain.user.application.dto.response.UserDetailsResponse
import com.weeth.domain.user.application.dto.response.UserInfoResponse
import com.weeth.domain.user.application.dto.response.UserProfileResponse
import com.weeth.domain.user.application.dto.response.UserSummaryResponse
import com.weeth.domain.user.application.exception.UserErrorCode
import com.weeth.domain.user.application.usecase.command.AdminUserUseCase
import com.weeth.domain.user.application.usecase.command.AuthUserUseCase
import com.weeth.domain.user.application.usecase.query.GetUserQueryService
import com.weeth.global.auth.annotation.CurrentUser
import com.weeth.global.auth.jwt.application.dto.JwtDto
import com.weeth.global.auth.jwt.application.exception.JwtErrorCode
import com.weeth.global.common.exception.ApiErrorCodeExample
import com.weeth.global.common.response.CommonResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.data.domain.Slice
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "USER", description = "사용자 API")
@RestController
@RequestMapping("/api/v4/users")
@ApiErrorCodeExample(UserErrorCode::class, JwtErrorCode::class)
class UserController(
    private val authUserUseCase: AuthUserUseCase,
    private val adminUserUseCase: AdminUserUseCase,
    private val getUserQueryService: GetUserQueryService,
) {
    @PostMapping("/social/kakao")
    @Operation(summary = "카카오 소셜 로그인(auth code flow)")
    fun socialLoginByKakao(
        @RequestBody @Valid request: SocialLoginRequest,
    ): CommonResponse<SocialLoginResponse> =
        CommonResponse.success(UserResponseCode.SOCIAL_LOGIN_SUCCESS, authUserUseCase.socialLoginByKakao(request))

    @PostMapping("/social/apple")
    @Operation(summary = "애플 소셜 로그인(auth code flow)")
    fun socialLoginByApple(
        @RequestBody @Valid request: SocialLoginRequest,
    ): CommonResponse<SocialLoginResponse> =
        CommonResponse.success(UserResponseCode.SOCIAL_LOGIN_SUCCESS, authUserUseCase.socialLoginByApple(request))

    @PostMapping("/social/refresh")
    @Operation(summary = "토큰 재발급")
    fun refreshToken(request: HttpServletRequest): CommonResponse<JwtDto> =
        CommonResponse.success(UserResponseCode.JWT_REFRESH_SUCCESS, authUserUseCase.refreshToken(request))

    @PostMapping("/apply")
    @Operation(summary = "동아리 지원 신청")
    fun apply(
        @RequestBody @Valid request: SignUpRequest,
    ): CommonResponse<Void> {
        authUserUseCase.apply(request)
        return CommonResponse.success(UserResponseCode.USER_APPLY_SUCCESS)
    }

    @GetMapping("/email")
    @Operation(summary = "이메일 중복 확인")
    fun checkEmail(
        @RequestParam email: String,
    ): CommonResponse<Boolean> =
        CommonResponse.success(UserResponseCode.USER_EMAIL_CHECK_SUCCESS, !getUserQueryService.existsByEmail(email))

    @GetMapping("/all")
    @Operation(summary = "동아리 멤버 전체 조회(전체/기수별)")
    fun findAllUser(
        @RequestParam("pageNumber") pageNumber: Int,
        @RequestParam("pageSize") pageSize: Int,
        @RequestParam(required = false) cardinal: Int?,
    ): CommonResponse<Slice<UserSummaryResponse>> =
        CommonResponse.success(UserResponseCode.USER_FIND_ALL_SUCCESS, getUserQueryService.findAllUser(pageNumber, pageSize, cardinal))

    @GetMapping("/search")
    @Operation(summary = "동아리 멤버 검색")
    fun searchUser(
        @RequestParam keyword: String,
    ): CommonResponse<List<UserSummaryResponse>> =
        CommonResponse.success(UserResponseCode.USER_FIND_BY_ID_SUCCESS, getUserQueryService.searchUser(keyword))

    @GetMapping("/details")
    @Operation(summary = "특정 멤버 상세 조회")
    fun findUser(
        @RequestParam userId: Long,
    ): CommonResponse<UserDetailsResponse> =
        CommonResponse.success(UserResponseCode.USER_DETAILS_SUCCESS, getUserQueryService.findUserDetails(userId))

    @GetMapping
    @Operation(summary = "내 정보 조회")
    fun find(
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<UserProfileResponse> =
        CommonResponse.success(UserResponseCode.USER_FIND_BY_ID_SUCCESS, getUserQueryService.findMyProfile(userId))

    @GetMapping("/info")
    @Operation(summary = "전역 내 정보 조회 API")
    fun findMyInfo(
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<UserInfoResponse> =
        CommonResponse.success(UserResponseCode.USER_FIND_BY_ID_SUCCESS, getUserQueryService.findMyInfo(userId))

    @PatchMapping
    @Operation(summary = "내 정보 수정")
    fun update(
        @RequestBody @Valid request: UpdateUserProfileRequest,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<Void> {
        authUserUseCase.updateProfile(request, userId)
        return CommonResponse.success(UserResponseCode.USER_UPDATE_SUCCESS)
    }

    @DeleteMapping
    @Operation(summary = "동아리 탈퇴")
    fun leave(
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<Void> {
        authUserUseCase.leave(userId)
        return CommonResponse.success(UserResponseCode.USER_LEAVE_SUCCESS)
    }
}
