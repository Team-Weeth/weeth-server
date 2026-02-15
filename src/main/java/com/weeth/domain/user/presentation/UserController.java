package com.weeth.domain.user.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import com.weeth.domain.user.application.dto.response.UserResponseDto;
import com.weeth.domain.user.application.dto.response.UserResponseDto.SummaryResponse;
import com.weeth.domain.user.application.dto.response.UserResponseDto.UserResponse;
import com.weeth.domain.user.application.exception.UserErrorCode;
import com.weeth.domain.user.application.usecase.UserManageUseCase;
import com.weeth.domain.user.application.usecase.UserUseCase;
import com.weeth.domain.user.domain.service.UserGetService;
import com.weeth.global.auth.annotation.CurrentUser;
import com.weeth.global.auth.jwt.application.dto.JwtDto;
import com.weeth.global.auth.jwt.exception.JwtErrorCode;
import com.weeth.global.common.exception.ApiErrorCodeExample;
import com.weeth.global.common.response.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.weeth.domain.user.application.dto.request.UserRequestDto.*;
import static com.weeth.domain.user.application.dto.response.UserResponseDto.Response;
import static com.weeth.domain.user.application.dto.response.UserResponseDto.SocialLoginResponse;
import static com.weeth.domain.user.presentation.UserResponseCode.*;

@Tag(name = "USER", description = "사용자 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
@ApiErrorCodeExample({UserErrorCode.class, JwtErrorCode.class})
public class UserController {

    private final UserUseCase userUseCase;
    private final UserManageUseCase userManageUseCase;
    private final UserGetService userGetService;

    @PostMapping("/kakao/login")
    @Operation(summary = "카카오 소셜 로그인 API")
    public CommonResponse<SocialLoginResponse> login(@RequestBody @Valid Login dto) {
        SocialLoginResponse response = userUseCase.login(dto);
        return CommonResponse.success(SOCIAL_LOGIN_SUCCESS, response);
    }

    @PostMapping("/kakao/auth")
    @Operation(summary = "카카오 소셜 회원가입 전 요청 API (미사용 API)")
    public CommonResponse<UserResponseDto.SocialAuthResponse> beforeRegister(@RequestBody @Valid Login dto) {
        UserResponseDto.SocialAuthResponse response = userUseCase.authenticate(dto);
        return CommonResponse.success(SOCIAL_AUTH_SUCCESS, response);
    }

    @PostMapping("/apply")
    @Operation(summary = "동아리 지원 신청. 현재 사용하지 않으므로 회원가입 시 /kakao/register api로 요청 바람")
    public CommonResponse<Void> apply(@RequestBody @Valid SignUp dto) {
        userUseCase.apply(dto);
        return CommonResponse.success(USER_APPLY_SUCCESS);
    }

    @PostMapping("/kakao/register")
    @Operation(summary = "소셜 회원가입")
    public CommonResponse<Void> register(@RequestBody @Valid Register dto) {
        userUseCase.socialRegister(dto);
        return CommonResponse.success(USER_APPLY_SUCCESS);
    }

    @PatchMapping("/kakao/link")
    @Operation(summary = "카카오 소셜 로그인 연동")
    public CommonResponse<SocialLoginResponse> integrate(@RequestBody @Valid NormalLogin dto) {
        return CommonResponse.success(SOCIAL_INTEGRATE_SUCCESS, userUseCase.integrate(dto));
    }

    @PostMapping("/apple/login")
    @Operation(summary = "애플 소셜 로그인 API")
    public CommonResponse<SocialLoginResponse> appleLogin(@RequestBody @Valid Login dto) {
        SocialLoginResponse response = userUseCase.appleLogin(dto);
        return CommonResponse.success(SOCIAL_LOGIN_SUCCESS, response);
    }

    @PostMapping("/apple/register")
    @Operation(summary = "애플 소셜 회원가입 (dev 전용 - 바로 ACTIVE)")
    public CommonResponse<Void> appleRegister(@RequestBody @Valid Register dto) {
        userUseCase.appleRegister(dto);
        return CommonResponse.success(USER_APPLY_SUCCESS);
    }

    @GetMapping("/email")
    @Operation(summary = "이메일 중복 확인")
    public CommonResponse<Boolean> checkEmail(@RequestParam String email) {
        return CommonResponse.success(USER_EMAIL_CHECK_SUCCESS, userGetService.check(email));
    }

    @GetMapping("/all")
    @Operation(summary = "동아리 멤버 전체 조회(전체/기수별)")
    public CommonResponse<Slice<SummaryResponse>> findAllUser(@RequestParam("pageNumber") int pageNumber,
                                                              @RequestParam("pageSize") int pageSize,
                                                              @RequestParam(required = false) Integer cardinal) {
        return CommonResponse.success(USER_FIND_ALL_SUCCESS, userUseCase.findAllUser(pageNumber, pageSize, cardinal));
    }

    @GetMapping("/search")
    @Operation(summary = "동아리 멤버 검색")
    public CommonResponse<List<SummaryResponse>> searchUser(@RequestParam String keyword) {
        return CommonResponse.success(USER_FIND_BY_ID_SUCCESS, userUseCase.searchUser(keyword));
    }

    @GetMapping("/details")
    @Operation(summary = "특정 멤버 상세 조회")
    public CommonResponse<UserResponse> findUser(@RequestParam Long userId) {
        return CommonResponse.success(
                USER_DETAILS_SUCCESS, userUseCase.findUserDetails(userId)
        );
    }

    @GetMapping
    @Operation(summary = "내 정보 조회")
    public CommonResponse<Response> find(@Parameter(hidden = true) @CurrentUser Long userId) {
        return CommonResponse.success(USER_FIND_BY_ID_SUCCESS, userUseCase.find(userId));
    }

    @GetMapping("/info")
    @Operation(summary = "전역 내 정보 조회 API")
    public CommonResponse<UserResponseDto.UserInfo> findMyInfo(@Parameter(hidden = true) @CurrentUser Long userId) {
        return CommonResponse.success(USER_FIND_BY_ID_SUCCESS, userUseCase.findUserInfo(userId));
    }

    @PatchMapping
    @Operation(summary = "내 정보 수정")
    public CommonResponse<Void> update(@RequestBody @Valid Update dto, @Parameter(hidden = true) @CurrentUser Long userId) {
        userUseCase.update(dto, userId);
        return CommonResponse.success(USER_UPDATE_SUCCESS);
    }

    @DeleteMapping
    @Operation(summary = "동아리 탈퇴")
    public CommonResponse<Void> leave(@Parameter(hidden = true) @CurrentUser Long userId) {
        userManageUseCase.leave(userId);
        return CommonResponse.success(USER_LEAVE_SUCCESS);
    }

    @PostMapping("/refresh")
    @Operation(summary = "JWT 토큰 재발급 API")
    public CommonResponse<JwtDto> refresh(@Parameter(hidden = true) @RequestHeader("Authorization_refresh") String refreshToken) {
        return CommonResponse.success(JWT_REFRESH_SUCCESS, userUseCase.refresh(refreshToken));
    }
}
