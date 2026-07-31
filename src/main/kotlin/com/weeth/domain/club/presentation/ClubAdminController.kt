package com.weeth.domain.club.presentation

import com.weeth.domain.club.application.dto.request.ClubMemberApplyObRequest
import com.weeth.domain.club.application.dto.request.ClubMemberRoleUpdateRequest
import com.weeth.domain.club.application.dto.request.ClubMemberSort
import com.weeth.domain.club.application.dto.request.ClubUpdateRequest
import com.weeth.domain.club.application.dto.request.UpdateMemberCardinalRequest
import com.weeth.domain.club.application.dto.response.ClubDetailResponse
import com.weeth.domain.club.application.dto.response.ClubMemberResponse
import com.weeth.domain.club.application.exception.ClubErrorCode
import com.weeth.domain.club.application.usecase.command.AdminClubMemberUseCase
import com.weeth.domain.club.application.usecase.command.ManageClubUseCase
import com.weeth.domain.club.application.usecase.query.GetClubMemberQueryService
import com.weeth.domain.club.application.usecase.query.GetClubQueryService
import com.weeth.global.auth.annotation.CurrentUser
import com.weeth.global.common.exception.ApiErrorCodeExample
import com.weeth.global.common.response.CommonResponse
import com.weeth.global.common.response.PageResponse
import com.weeth.global.common.web.TsidParam
import com.weeth.global.common.web.TsidPathVariable
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "CLUB-ADMIN", description = "동아리 관리자 API")
@RestController
@RequestMapping("/api/v4/admin/clubs/{clubId}")
@ApiErrorCodeExample(ClubErrorCode::class)
class ClubAdminController(
    private val manageClubUseCase: ManageClubUseCase,
    private val adminClubMemberUseCase: AdminClubMemberUseCase,
    private val getClubQueryService: GetClubQueryService,
    private val getClubMemberQueryService: GetClubMemberQueryService,
) {
    @GetMapping
    @Operation(summary = "동아리 상세 정보 조회")
    fun getClubDetail(
        @Parameter(hidden = true) @CurrentUser userId: Long,
        @TsidParam
        @TsidPathVariable clubId: Long,
    ): CommonResponse<ClubDetailResponse> {
        val detail = getClubQueryService.findClubDetailForAdmin(clubId, userId)
        return CommonResponse.success(ClubResponseCode.CLUB_FIND_BY_ID_SUCCESS, detail)
    }

    @PatchMapping
    @Operation(summary = "동아리 정보 수정")
    fun update(
        @Parameter(hidden = true) @CurrentUser userId: Long,
        @TsidParam
        @TsidPathVariable clubId: Long,
        @Valid @RequestBody request: ClubUpdateRequest,
    ): CommonResponse<Unit> {
        manageClubUseCase.update(clubId, userId, request)
        return CommonResponse.success(ClubResponseCode.CLUB_UPDATED_SUCCESS)
    }

    @DeleteMapping("/profile-image")
    @Operation(summary = "동아리 프로필 사진 삭제")
    fun deleteProfileImage(
        @Parameter(hidden = true) @CurrentUser userId: Long,
        @TsidParam
        @TsidPathVariable clubId: Long,
    ): CommonResponse<Unit> {
        manageClubUseCase.deleteProfileImage(clubId, userId)
        return CommonResponse.success(ClubResponseCode.CLUB_PROFILE_IMAGE_DELETED_SUCCESS)
    }

    @DeleteMapping("/background-image")
    @Operation(summary = "동아리 배경 사진 삭제")
    fun deleteBackgroundImage(
        @Parameter(hidden = true) @CurrentUser userId: Long,
        @TsidParam
        @TsidPathVariable clubId: Long,
    ): CommonResponse<Unit> {
        manageClubUseCase.deleteBackgroundImage(clubId, userId)
        return CommonResponse.success(ClubResponseCode.CLUB_BACKGROUND_IMAGE_DELETED_SUCCESS)
    }

    @PostMapping("/code/regenerate")
    @Operation(summary = "초대 코드 재생성 (MVP 미사용)", deprecated = true)
    fun regenerateCode(
        @Parameter(hidden = true) @CurrentUser userId: Long,
        @TsidParam
        @TsidPathVariable clubId: Long,
    ): CommonResponse<Unit> {
        manageClubUseCase.regenerateCode(clubId, userId)
        return CommonResponse.success(ClubResponseCode.CLUB_CODE_REGENERATED_SUCCESS)
    }

    @GetMapping("/members")
    @Operation(
        summary = "동아리 멤버 목록 조회",
        description = "기수 필터, 이름·학과·학번 검색, 정렬, 페이지네이션을 지원합니다. 가입 대기·추방·탈퇴 멤버도 포함됩니다.",
    )
    fun getClubMembers(
        @Parameter(hidden = true) @CurrentUser userId: Long,
        @TsidParam
        @TsidPathVariable clubId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) cardinalNumber: Int?,
        @RequestParam(defaultValue = "CARDINAL_DESC") sort: ClubMemberSort,
    ): CommonResponse<PageResponse<ClubMemberResponse>> {
        val members =
            getClubMemberQueryService.findClubMembersForAdmin(
                clubId = clubId,
                userId = userId,
                page = page,
                size = size,
                keyword = keyword,
                cardinalNumber = cardinalNumber,
                sort = sort,
            )
        return CommonResponse.success(ClubResponseCode.MEMBER_FIND_ALL_SUCCESS, members)
    }

    @GetMapping("/members/{clubMemberId}")
    @Operation(summary = "동아리 멤버 상세 조회", description = "가입 대기·추방·탈퇴 멤버도 조회할 수 있습니다.")
    fun getClubMemberDetail(
        @Parameter(hidden = true) @CurrentUser userId: Long,
        @TsidParam
        @TsidPathVariable clubId: Long,
        @PathVariable clubMemberId: Long,
    ): CommonResponse<ClubMemberResponse> {
        val member = getClubMemberQueryService.findClubMemberDetailForAdmin(clubId, userId, clubMemberId)
        return CommonResponse.success(ClubResponseCode.MEMBER_FIND_DETAIL_SUCCESS, member)
    }

    @PatchMapping("/members/{clubMemberId}/accept")
    @Operation(summary = "멤버 승인", deprecated = true)
    fun acceptMember(
        @Parameter(hidden = true) @CurrentUser userId: Long,
        @TsidParam
        @TsidPathVariable clubId: Long,
        @PathVariable clubMemberId: Long,
    ): CommonResponse<Unit> {
        adminClubMemberUseCase.accept(clubId, userId, clubMemberId)
        return CommonResponse.success(ClubResponseCode.MEMBER_ACCEPTED_SUCCESS)
    }

    @DeleteMapping("/members/{clubMemberId}/ban")
    @Operation(summary = "멤버 추방")
    fun banMember(
        @Parameter(hidden = true) @CurrentUser userId: Long,
        @TsidParam
        @TsidPathVariable clubId: Long,
        @PathVariable clubMemberId: Long,
    ): CommonResponse<Unit> {
        adminClubMemberUseCase.ban(clubId, userId, clubMemberId)
        return CommonResponse.success(ClubResponseCode.MEMBER_BANNED_SUCCESS)
    }

    @PatchMapping("/members/{clubMemberId}/restore")
    @Operation(summary = "추방 멤버 복구")
    fun restoreMember(
        @Parameter(hidden = true) @CurrentUser userId: Long,
        @TsidParam
        @TsidPathVariable clubId: Long,
        @PathVariable clubMemberId: Long,
    ): CommonResponse<Unit> {
        adminClubMemberUseCase.restore(clubId, userId, clubMemberId)
        return CommonResponse.success(ClubResponseCode.MEMBER_RESTORED_SUCCESS)
    }

    @PatchMapping("/members/{clubMemberId}/role")
    @Operation(summary = "멤버 권한 변경")
    fun updateMemberRole(
        @Parameter(hidden = true) @CurrentUser userId: Long,
        @TsidParam
        @TsidPathVariable clubId: Long,
        @PathVariable clubMemberId: Long,
        @Valid @RequestBody request: ClubMemberRoleUpdateRequest,
    ): CommonResponse<Unit> {
        adminClubMemberUseCase.updateMemberRole(clubId, userId, clubMemberId, request)
        return CommonResponse.success(ClubResponseCode.MEMBER_ROLE_UPDATED_SUCCESS)
    }

    @PatchMapping("/members/{targetClubMemberId}/lead")
    @Operation(summary = "LEAD 권한 이양")
    fun transferLead(
        @TsidParam
        @TsidPathVariable clubId: Long,
        @Parameter(hidden = true) @CurrentUser userId: Long,
        @PathVariable targetClubMemberId: Long,
    ): CommonResponse<Unit> {
        adminClubMemberUseCase.transferLead(clubId, userId, targetClubMemberId)
        return CommonResponse.success(ClubResponseCode.LEAD_TRANSFERRED_SUCCESS)
    }

    @PatchMapping("/members/{clubMemberId}/cardinals")
    @Operation(summary = "멤버 기수 수정")
    @ApiErrorCodeExample(ClubErrorCode::class)
    fun updateMemberCardinals(
        @Parameter(hidden = true) @CurrentUser userId: Long,
        @TsidParam
        @TsidPathVariable clubId: Long,
        @PathVariable clubMemberId: Long,
        @Valid @RequestBody request: UpdateMemberCardinalRequest,
    ): CommonResponse<Unit> {
        adminClubMemberUseCase.updateCardinals(clubId, userId, clubMemberId, request)
        return CommonResponse.success(ClubResponseCode.MEMBER_CARDINAL_UPDATED_SUCCESS)
    }

    @PatchMapping("/members/apply-ob")
    @Operation(summary = "멤버 OB 기수 등록", deprecated = true)
    fun applyOb(
        @Parameter(hidden = true) @CurrentUser userId: Long,
        @TsidParam
        @TsidPathVariable clubId: Long,
        @Valid @RequestBody requests: List<ClubMemberApplyObRequest>,
    ): CommonResponse<Unit> {
        adminClubMemberUseCase.applyOb(clubId, userId, requests)
        return CommonResponse.success(ClubResponseCode.MEMBER_APPLY_OB_SUCCESS)
    }
}
