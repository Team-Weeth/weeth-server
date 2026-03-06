package com.weeth.domain.club.presentation

import com.weeth.domain.club.application.dto.request.ClubMemberRoleUpdateRequest
import com.weeth.domain.club.application.dto.request.ClubUpdateRequest
import com.weeth.domain.club.application.dto.response.ClubDetailResponse
import com.weeth.domain.club.application.dto.response.ClubMemberResponse
import com.weeth.domain.club.application.exception.ClubErrorCode
import com.weeth.domain.club.application.usecase.command.AdminClubMemberUseCase
import com.weeth.domain.club.application.usecase.command.ManageClubUseCase
import com.weeth.domain.club.application.usecase.query.GetClubMemberQueryService
import com.weeth.domain.club.application.usecase.query.GetClubQueryService
import com.weeth.global.auth.annotation.CurrentUser
import com.weeth.global.common.exception.ApiErrorCodeExample
import com.weeth.global.common.id.TsidBase62Encoder
import com.weeth.global.common.response.CommonResponse
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
        @PathVariable clubId: String,
    ): CommonResponse<ClubDetailResponse> {
        val decodedClubId = TsidBase62Encoder.decode(clubId)
        val detail = getClubQueryService.findClubDetailForAdmin(decodedClubId, userId)
        return CommonResponse.success(ClubResponseCode.CLUB_FIND_BY_ID, detail)
    }

    @PatchMapping
    @Operation(summary = "동아리 정보 수정")
    fun update(
        @Parameter(hidden = true) @CurrentUser userId: Long,
        @PathVariable clubId: String,
        @Valid @RequestBody request: ClubUpdateRequest,
    ): CommonResponse<Unit> {
        val decodedClubId = TsidBase62Encoder.decode(clubId)
        manageClubUseCase.update(decodedClubId, userId, request)
        return CommonResponse.success(ClubResponseCode.CLUB_UPDATED)
    }

    @PostMapping("/code/regenerate")
    @Operation(summary = "초대 코드 재생성 (MVP 미사용)", deprecated = true)
    fun regenerateCode(
        @Parameter(hidden = true) @CurrentUser userId: Long,
        @PathVariable clubId: String,
    ): CommonResponse<Unit> {
        val decodedClubId = TsidBase62Encoder.decode(clubId)
        manageClubUseCase.regenerateCode(decodedClubId, userId)
        return CommonResponse.success(ClubResponseCode.CLUB_CODE_REGENERATED)
    }

    @GetMapping("/members")
    @Operation(summary = "동아리 멤버 목록 조회")
    fun getClubMembers(
        @Parameter(hidden = true) @CurrentUser userId: Long,
        @PathVariable clubId: String,
    ): CommonResponse<List<ClubMemberResponse>> {
        val decodedClubId = TsidBase62Encoder.decode(clubId)
        val members = getClubMemberQueryService.findClubMembersForAdmin(decodedClubId, userId)
        return CommonResponse.success(ClubResponseCode.MEMBER_FIND_ALL, members)
    }

    @PatchMapping("/members/{clubMemberId}/accept")
    @Operation(summary = "멤버 승인")
    fun acceptMember(
        @Parameter(hidden = true) @CurrentUser userId: Long,
        @PathVariable clubId: String,
        @PathVariable clubMemberId: Long,
    ): CommonResponse<Unit> {
        val decodedClubId = TsidBase62Encoder.decode(clubId)
        adminClubMemberUseCase.accept(decodedClubId, userId, clubMemberId)
        return CommonResponse.success(ClubResponseCode.MEMBER_ACCEPTED)
    }

    @DeleteMapping("/members/{clubMemberId}/ban")
    @Operation(summary = "멤버 추방")
    fun banMember(
        @Parameter(hidden = true) @CurrentUser userId: Long,
        @PathVariable clubId: String,
        @PathVariable clubMemberId: Long,
    ): CommonResponse<Unit> {
        val decodedClubId = TsidBase62Encoder.decode(clubId)
        adminClubMemberUseCase.ban(decodedClubId, userId, clubMemberId)
        return CommonResponse.success(ClubResponseCode.MEMBER_BANNED)
    }

    @PatchMapping("/members/{clubMemberId}/role")
    @Operation(summary = "멤버 권한 변경")
    fun updateMemberRole(
        @Parameter(hidden = true) @CurrentUser userId: Long,
        @PathVariable clubId: String,
        @PathVariable clubMemberId: Long,
        @Valid @RequestBody request: ClubMemberRoleUpdateRequest,
    ): CommonResponse<Unit> {
        val decodedClubId = TsidBase62Encoder.decode(clubId)
        adminClubMemberUseCase.updateMemberRole(decodedClubId, userId, request)
        return CommonResponse.success(ClubResponseCode.MEMBER_ROLE_UPDATED)
    }
}
