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
import com.weeth.global.auth.jwt.application.service.TokenCookieProvider
import com.weeth.global.common.exception.ApiErrorCodeExample
import com.weeth.global.common.response.CommonResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
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
    private val tokenCookieProvider: TokenCookieProvider,
) {
    @PostMapping("/social/kakao")
    @Operation(summary = "카카오 소셜 로그인(auth code flow)")
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
}
