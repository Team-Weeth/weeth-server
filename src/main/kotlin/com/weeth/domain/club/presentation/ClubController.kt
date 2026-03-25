package com.weeth.domain.club.presentation

import com.weeth.domain.club.application.dto.request.ClubCreateRequest
import com.weeth.domain.club.application.dto.response.ClubInfoResponse
import com.weeth.domain.club.application.dto.response.ClubMembershipStatusResponse
import com.weeth.domain.club.application.dto.response.ClubPublicResponse
import com.weeth.domain.club.application.exception.ClubErrorCode
import com.weeth.domain.club.application.usecase.command.ManageClubUseCase
import com.weeth.domain.club.application.usecase.query.GetClubQueryService
import com.weeth.global.auth.annotation.CurrentUser
import com.weeth.global.common.exception.ApiErrorCodeExample
import com.weeth.global.common.response.CommonResponse
import com.weeth.global.common.web.TsidParam
import com.weeth.global.common.web.TsidPathVariable
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "CLUB", description = "동아리 API")
@RestController
@RequestMapping("/api/v4/clubs")
@ApiErrorCodeExample(ClubErrorCode::class)
class ClubController(
    private val manageClubUseCase: ManageClubUseCase,
    private val getClubQueryService: GetClubQueryService,
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
    @Operation(summary = "내가 가입한 동아리 목록 조회")
    fun getMyClubs(
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<List<ClubInfoResponse>> {
        val clubs = getClubQueryService.findMyClubs(userId)

        return CommonResponse.success(ClubResponseCode.CLUB_FIND_ALL_SUCCESS, clubs)
    }

    @GetMapping("/{clubId}")
    @Operation(summary = "동아리 공개 정보 조회 (이름, 소개, 프로필 사진) - 인증 불필요")
    @SecurityRequirements
    fun getClubPublicInfo(
        @TsidParam
        @TsidPathVariable clubId: Long,
    ): CommonResponse<ClubPublicResponse> {
        val info = getClubQueryService.findClub(clubId)

        return CommonResponse.success(ClubResponseCode.CLUB_FIND_SUCCESS, info)
    }

    @GetMapping("/membership-status")
    @Operation(summary = "동아리 가입 여부 조회")
    fun getMembershipStatus(
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<ClubMembershipStatusResponse> {
        val status = getClubQueryService.findMembershipStatus(userId)

        return CommonResponse.success(ClubResponseCode.MEMBERSHIP_STATUS_FIND_SUCCESS, status)
    }
}
