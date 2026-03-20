package com.weeth.domain.club.presentation

import com.weeth.domain.club.application.dto.request.ClubCreateRequest
import com.weeth.domain.club.application.dto.request.ClubJoinRequest
import com.weeth.domain.club.application.dto.request.UpdateMemberProfileRequest
import com.weeth.domain.club.application.dto.response.ClubInfoResponse
import com.weeth.domain.club.application.dto.response.ClubMemberProfileResponse
import com.weeth.domain.club.application.dto.response.ClubMemberResponse
import com.weeth.domain.club.application.dto.response.ClubResponse
import com.weeth.domain.club.application.exception.ClubErrorCode
import com.weeth.domain.club.application.usecase.command.ManageClubMemberUsecase
import com.weeth.domain.club.application.usecase.command.ManageClubUseCase
import com.weeth.domain.club.application.usecase.query.GetClubMemberQueryService
import com.weeth.domain.club.application.usecase.query.GetClubQueryService
import com.weeth.global.auth.annotation.CurrentUser
import com.weeth.global.common.exception.ApiErrorCodeExample
import com.weeth.global.common.response.CommonResponse
import com.weeth.global.common.web.TsidParam
import com.weeth.global.common.web.TsidPathVariable
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "CLUB", description = "동아리 API")
@RestController
@RequestMapping("/api/v4/clubs")
@ApiErrorCodeExample(ClubErrorCode::class)
class ClubController(
    private val manageClubUseCase: ManageClubUseCase,
    private val manageClubMemberUsecase: ManageClubMemberUsecase,
    private val getClubQueryService: GetClubQueryService,
    private val getClubMemberQueryService: GetClubMemberQueryService,
) {
    @PostMapping
    @Operation(summary = "동아리 생성")
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @Parameter(hidden = true) @CurrentUser userId: Long,
        @Valid @RequestBody request: ClubCreateRequest,
    ): CommonResponse<Unit> {
        manageClubUseCase.create(userId, request)

        return CommonResponse.success(ClubResponseCode.CLUB_CREATED_SUCCESS)
    }

    @GetMapping
    @Operation(summary = "내가 가입한 동아리 목록 조회 (MVP 미사용)", deprecated = true)
    fun getMyClubs(
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<List<ClubInfoResponse>> {
        val clubs = getClubQueryService.findMyClubs(userId)

        return CommonResponse.success(ClubResponseCode.CLUB_FIND_ALL_SUCCESS, clubs)
    }

    @GetMapping("/{clubId}")
    @Operation(summary = "동아리 정보 조회 (이름, 소개, 이미지)")
    fun getClubPublicInfo(
        @Parameter(hidden = true) @CurrentUser userId: Long,
        @PathVariable @TsidParam
        @TsidPathVariable clubId: Long,
    ): CommonResponse<ClubResponse> {
        val info = getClubQueryService.findClub(clubId)

        return CommonResponse.success(ClubResponseCode.CLUB_FIND_SUCCESS, info)
    }

    @PostMapping("/{clubId}/join")
    @Operation(summary = "동아리 가입")
    fun join(
        @Parameter(hidden = true) @CurrentUser userId: Long,
        @PathVariable @TsidParam
        @TsidPathVariable clubId: Long,
        @Valid @RequestBody request: ClubJoinRequest,
    ): CommonResponse<Unit> {
        manageClubMemberUsecase.join(clubId, userId, request)

        return CommonResponse.success(ClubResponseCode.CLUB_JOINED_SUCCESS)
    }

    @DeleteMapping("/{clubId}/leave")
    @Operation(summary = "동아리 탈퇴")
    fun leave(
        @Parameter(hidden = true) @CurrentUser userId: Long,
        @PathVariable @TsidParam
        @TsidPathVariable clubId: Long,
    ): CommonResponse<Unit> {
        manageClubMemberUsecase.leave(clubId, userId)

        return CommonResponse.success(ClubResponseCode.CLUB_LEFT_SUCCESS)
    }

    @GetMapping("/{clubId}/members/me")
    @Operation(summary = "내 멤버 정보 조회")
    fun getMyMemberInfo(
        @Parameter(hidden = true) @CurrentUser userId: Long,
        @PathVariable @TsidParam
        @TsidPathVariable clubId: Long,
    ): CommonResponse<ClubMemberProfileResponse> {
        val meInfo = getClubMemberQueryService.findMyMemberProfile(clubId, userId)

        return CommonResponse.success(ClubResponseCode.MEMBER_FIND_ME_SUCCESS, meInfo)
    }

    // TODO: 추후 동아리별 프로필 수정으로 변경 시 clubId 경로 변수 추가 및 단일 ClubMember만 수정하도록 변경
    @PatchMapping("/members/me")
    @Operation(summary = "내 클럽 활동 프로필 수정 (프로필 사진, 자기소개)")
    fun updateMyProfile(
        @Parameter(hidden = true) @CurrentUser userId: Long,
        @Valid @RequestBody request: UpdateMemberProfileRequest,
    ): CommonResponse<Unit> {
        manageClubMemberUsecase.updateProfile(userId, request)
        return CommonResponse.success(ClubResponseCode.MEMBER_PROFILE_UPDATED_SUCCESS)
    }

    // TODO: 추후 동아리별 프로필 수정으로 변경 시 clubId 경로 변수 추가 및 단일 ClubMember만 수정하도록 변경
    @DeleteMapping("/members/me/profile-image")
    @Operation(summary = "동아리 프로필 사진 삭제")
    fun deleteMyProfileImage(
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<Unit> {
        manageClubMemberUsecase.deleteProfileImage(userId)
        return CommonResponse.success(ClubResponseCode.MEMBER_PROFILE_IMAGE_DELETED_SUCCESS)
    }

    // TODO: MVP 후 동아리 멤버 조회 기능 구현
}
