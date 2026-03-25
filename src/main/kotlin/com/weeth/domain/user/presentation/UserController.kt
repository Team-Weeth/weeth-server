package com.weeth.domain.user.presentation

import com.weeth.domain.user.application.dto.request.AgreeTermsRequest
import com.weeth.domain.user.application.dto.request.SocialLoginRequest
import com.weeth.domain.user.application.dto.request.UpdateUserProfileRequest
import com.weeth.domain.user.application.dto.response.SocialLoginResponse
import com.weeth.domain.user.application.exception.UserErrorCode
import com.weeth.domain.user.application.usecase.command.AgreeTermsUseCase
import com.weeth.domain.user.application.usecase.command.AuthUserUseCase
import com.weeth.domain.user.application.usecase.command.SocialLoginUseCase
import com.weeth.domain.user.application.usecase.command.UpdateUserProfileUseCase
import com.weeth.global.auth.annotation.CurrentUser
import com.weeth.global.auth.jwt.application.dto.JwtDto
import com.weeth.global.auth.jwt.application.exception.JwtErrorCode
import com.weeth.global.common.exception.ApiErrorCodeExample
import com.weeth.global.common.response.CommonResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PatchMapping
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
) {
    @PostMapping("/social/kakao")
    @Operation(summary = "카카오 소셜 로그인(auth code flow)")
    @SecurityRequirements
    fun socialLoginByKakao(
        @RequestBody @Valid request: SocialLoginRequest,
    ): CommonResponse<SocialLoginResponse> =
        CommonResponse.success(UserResponseCode.SOCIAL_LOGIN_SUCCESS, socialLoginUseCase.socialLoginByKakao(request))

    @PostMapping("/social/apple")
    @Operation(summary = "애플 소셜 로그인(auth code flow)")
    @SecurityRequirements
    fun socialLoginByApple(
        @RequestBody @Valid request: SocialLoginRequest,
    ): CommonResponse<SocialLoginResponse> =
        CommonResponse.success(UserResponseCode.SOCIAL_LOGIN_SUCCESS, socialLoginUseCase.socialLoginByApple(request))

    @PostMapping("/social/refresh")
    @Operation(summary = "토큰 재발급")
    @SecurityRequirements
    fun refreshToken(request: HttpServletRequest): CommonResponse<JwtDto> =
        CommonResponse.success(UserResponseCode.JWT_REFRESH_SUCCESS, authUserUseCase.refreshToken(request))

    @PostMapping("/terms")
    @Operation(summary = "약관 동의")
    fun agreeTerms(
        @RequestBody @Valid request: AgreeTermsRequest,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<Void?> {
        agreeTermsUseCase.execute(userId, request)
        return CommonResponse.success(UserResponseCode.USER_TERMS_AGREE_SUCCESS)
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
}
