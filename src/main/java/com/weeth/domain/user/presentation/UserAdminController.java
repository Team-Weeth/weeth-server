package com.weeth.domain.user.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.weeth.domain.user.application.exception.UserErrorCode;
import com.weeth.domain.user.application.usecase.UserManageUseCase;
import com.weeth.domain.user.domain.entity.enums.UsersOrderBy;
import com.weeth.global.auth.jwt.exception.JwtErrorCode;
import com.weeth.global.common.exception.ApiErrorCodeExample;
import com.weeth.global.common.response.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.weeth.domain.user.application.dto.request.UserRequestDto.*;
import static com.weeth.domain.user.application.dto.response.UserResponseDto.AdminResponse;
import static com.weeth.domain.user.presentation.UserResponseCode.*;

@Tag(name = "USER ADMIN", description = "[ADMIN] 사용자 어드민 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/users")
@ApiErrorCodeExample({UserErrorCode.class, JwtErrorCode.class})
public class UserAdminController {

    private final UserManageUseCase userManageUseCase;

    @GetMapping("/all")
    @Operation(summary = "어드민용 회원 조회")
    public CommonResponse<List<AdminResponse>> findAll(@RequestParam UsersOrderBy orderBy) {
        return CommonResponse.success(USER_FIND_ALL_SUCCESS, userManageUseCase.findAllByAdmin(orderBy));
    }

    @PatchMapping
    @Operation(summary = "가입 신청 승인")
    public CommonResponse<Void> accept(@RequestBody UserId userId) {
        userManageUseCase.accept(userId);
        return CommonResponse.success(USER_ACCEPT_SUCCESS);
    }

    @DeleteMapping
    @Operation(summary = "유저 추방")
    public CommonResponse<Void> ban(@RequestBody UserId userId) {
        userManageUseCase.ban(userId);
        return CommonResponse.success(USER_BAN_SUCCESS);
    }

    @PatchMapping("/role")
    @Operation(summary = "관리자로 승격/강등")
    public CommonResponse<Void> update(@RequestBody List<UserRoleUpdate> request) {
        userManageUseCase.update(request);
        return CommonResponse.success(USER_ROLE_UPDATE_SUCCESS);
    }

    @PatchMapping("/apply")
    @Operation(summary = "다음 기수도 이어서 진행")
    public CommonResponse<Void> applyOB(@RequestBody List<UserApplyOB> request) {
        userManageUseCase.applyOB(request);
        return CommonResponse.success(USER_APPLY_OB_SUCCESS);
    }

    @PatchMapping("/reset")
    @Operation(summary = "회원 비밀번호 초기화")
    public CommonResponse<Void> resetPassword(@RequestBody UserId userId) {
        userManageUseCase.reset(userId);
        return CommonResponse.success(USER_PASSWORD_RESET_SUCCESS);
    }
}
