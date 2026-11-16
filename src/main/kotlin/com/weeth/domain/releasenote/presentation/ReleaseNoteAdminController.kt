package com.weeth.domain.releasenote.presentation

import com.weeth.domain.releasenote.application.dto.request.CreateReleaseNoteRequest
import com.weeth.domain.releasenote.application.dto.request.UpdateReleaseNoteRequest
import com.weeth.domain.releasenote.application.dto.request.UpdateReleaseNoteStatusRequest
import com.weeth.domain.releasenote.application.dto.response.ReleaseNoteIdResponse
import com.weeth.domain.releasenote.application.dto.response.ReleaseNoteResponse
import com.weeth.domain.releasenote.application.dto.response.ReleaseNoteSummaryResponse
import com.weeth.domain.releasenote.application.exception.ReleaseNoteErrorCode
import com.weeth.domain.releasenote.application.usecase.command.ManageReleaseNoteUseCase
import com.weeth.domain.releasenote.application.usecase.query.GetReleaseNoteAdminQueryService
import com.weeth.domain.releasenote.domain.enums.ReleaseNoteStatus
import com.weeth.global.auth.annotation.CurrentUser
import com.weeth.global.auth.jwt.application.exception.JwtErrorCode
import com.weeth.global.common.exception.ApiErrorCodeExample
import com.weeth.global.common.response.CommonResponse
import com.weeth.global.common.response.SliceResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "RELEASE_NOTE_ADMIN", description = "릴리즈 노트 백오피스 API (DEVELOPER 전용)")
@RestController
@RequestMapping("/api/v4/admin/release-notes")
@ApiErrorCodeExample(ReleaseNoteErrorCode::class, JwtErrorCode::class)
class ReleaseNoteAdminController(
    private val manageReleaseNoteUseCase: ManageReleaseNoteUseCase,
    private val getReleaseNoteAdminQueryService: GetReleaseNoteAdminQueryService,
) {
    @GetMapping
    @Operation(summary = "릴리즈 노트 백오피스 목록 조회 (DRAFT 포함)")
    fun getReleaseNotes(
        @Parameter(description = "발행 상태 필터. 없으면 전체", example = "DRAFT")
        @RequestParam(required = false) status: ReleaseNoteStatus?,
        @Parameter(description = "페이지 번호", example = "0")
        @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "페이지 크기", example = "12")
        @RequestParam(defaultValue = "12") size: Int,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<SliceResponse<ReleaseNoteSummaryResponse>> =
        CommonResponse.success(
            ReleaseNoteResponseCode.RELEASE_NOTE_ADMIN_LIST_SUCCESS,
            getReleaseNoteAdminQueryService.findAll(userId, status, page, size),
        )

    @GetMapping("/{releaseNoteId}")
    @Operation(summary = "릴리즈 노트 백오피스 상세 조회 (DRAFT 포함)")
    fun getReleaseNote(
        @PathVariable releaseNoteId: Long,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<ReleaseNoteResponse> =
        CommonResponse.success(
            ReleaseNoteResponseCode.RELEASE_NOTE_ADMIN_DETAIL_SUCCESS,
            getReleaseNoteAdminQueryService.find(userId, releaseNoteId),
        )

    @PostMapping
    @Operation(summary = "릴리즈 노트 작성")
    fun createReleaseNote(
        @RequestBody @Valid request: CreateReleaseNoteRequest,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<ReleaseNoteIdResponse> =
        CommonResponse.success(
            ReleaseNoteResponseCode.RELEASE_NOTE_CREATED_SUCCESS,
            manageReleaseNoteUseCase.create(userId, request),
        )

    @PutMapping("/{releaseNoteId}")
    @Operation(summary = "릴리즈 노트 수정 (전체 교체, 발행 상태 유지)")
    fun updateReleaseNote(
        @PathVariable releaseNoteId: Long,
        @RequestBody @Valid request: UpdateReleaseNoteRequest,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<Void?> {
        manageReleaseNoteUseCase.update(userId, releaseNoteId, request)
        return CommonResponse.success(ReleaseNoteResponseCode.RELEASE_NOTE_UPDATED_SUCCESS)
    }

    @PatchMapping("/{releaseNoteId}/status")
    @Operation(summary = "릴리즈 노트 발행 상태 변경")
    fun updateReleaseNoteStatus(
        @PathVariable releaseNoteId: Long,
        @RequestBody @Valid request: UpdateReleaseNoteStatusRequest,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<Void?> {
        manageReleaseNoteUseCase.updateStatus(userId, releaseNoteId, request)
        return CommonResponse.success(ReleaseNoteResponseCode.RELEASE_NOTE_STATUS_UPDATED_SUCCESS)
    }

    @DeleteMapping("/{releaseNoteId}")
    @Operation(summary = "릴리즈 노트 삭제")
    fun deleteReleaseNote(
        @PathVariable releaseNoteId: Long,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<Void?> {
        manageReleaseNoteUseCase.delete(userId, releaseNoteId)
        return CommonResponse.success(ReleaseNoteResponseCode.RELEASE_NOTE_DELETED_SUCCESS)
    }
}
