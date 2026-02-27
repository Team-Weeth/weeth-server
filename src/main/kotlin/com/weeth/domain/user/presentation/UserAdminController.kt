package com.weeth.domain.user.presentation

import com.weeth.domain.user.application.dto.request.UserApplyObRequest
import com.weeth.domain.user.application.dto.request.UserIdsRequest
import com.weeth.domain.user.application.dto.request.UserRoleUpdateRequest
import com.weeth.domain.user.application.dto.response.AdminUserResponse
import com.weeth.domain.user.application.exception.UserErrorCode
import com.weeth.domain.user.application.usecase.command.AdminUserUseCase
import com.weeth.domain.user.application.usecase.query.GetUserQueryService
import com.weeth.domain.user.domain.entity.enums.UsersOrderBy
import com.weeth.global.auth.jwt.application.exception.JwtErrorCode
import com.weeth.global.common.exception.ApiErrorCodeExample
import com.weeth.global.common.response.CommonResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "USER ADMIN", description = "[ADMIN] 사용자 어드민 API")
@RestController
@RequestMapping("/api/v4/admin/users")
@ApiErrorCodeExample(UserErrorCode::class, JwtErrorCode::class)
class UserAdminController(
    private val adminUserUseCase: AdminUserUseCase,
    private val getUserQueryService: GetUserQueryService,
) {
    @GetMapping("/all")
    @Operation(summary = "어드민용 회원 조회")
    fun findAll(
        @RequestParam orderBy: UsersOrderBy,
    ): CommonResponse<List<AdminUserResponse>> =
        CommonResponse.success(UserResponseCode.USER_FIND_ALL_SUCCESS, getUserQueryService.findAllByAdmin(orderBy))

    @PatchMapping
    @Operation(summary = "가입 신청 승인")
    fun accept(
        @RequestBody @Valid request: UserIdsRequest,
    ): CommonResponse<Void> {
        adminUserUseCase.accept(request)
        return CommonResponse.success(UserResponseCode.USER_ACCEPT_SUCCESS)
    }

    @DeleteMapping
    @Operation(summary = "유저 추방")
    fun ban(
        @RequestBody @Valid request: UserIdsRequest,
    ): CommonResponse<Void> {
        adminUserUseCase.ban(request)
        return CommonResponse.success(UserResponseCode.USER_BAN_SUCCESS)
    }

    @PatchMapping("/role")
    @Operation(summary = "관리자로 승격/강등")
    fun update(
        @RequestBody request: List<UserRoleUpdateRequest>,
    ): CommonResponse<Void> {
        adminUserUseCase.updateRole(request)
        return CommonResponse.success(UserResponseCode.USER_ROLE_UPDATE_SUCCESS)
    }

    @PatchMapping("/apply")
    @Operation(summary = "다음 기수도 이어서 진행")
    fun applyOb(
        @RequestBody request: List<UserApplyObRequest>,
    ): CommonResponse<Void> {
        adminUserUseCase.applyOb(request)
        return CommonResponse.success(UserResponseCode.USER_APPLY_OB_SUCCESS)
    }
}
