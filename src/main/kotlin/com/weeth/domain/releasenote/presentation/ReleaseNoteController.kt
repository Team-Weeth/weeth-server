package com.weeth.domain.releasenote.presentation

import com.weeth.domain.releasenote.application.dto.response.ReleaseNoteResponse
import com.weeth.domain.releasenote.application.dto.response.ReleaseNoteSummaryResponse
import com.weeth.domain.releasenote.application.exception.ReleaseNoteErrorCode
import com.weeth.domain.releasenote.application.usecase.query.GetReleaseNoteQueryService
import com.weeth.global.common.exception.ApiErrorCodeExample
import com.weeth.global.common.response.CommonResponse
import com.weeth.global.common.response.SliceResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "RELEASE_NOTE", description = "릴리즈 노트 공개 조회 API (비로그인 포함)")
@RestController
@RequestMapping("/api/v4/release-notes")
@ApiErrorCodeExample(ReleaseNoteErrorCode::class)
class ReleaseNoteController(
    private val getReleaseNoteQueryService: GetReleaseNoteQueryService,
) {
    @GetMapping
    @Operation(summary = "릴리즈 노트 목록 조회 (발행된 노트만, 비로그인 허용)")
    fun getReleaseNotes(
        @Parameter(description = "제목/본문 검색어. 없으면 전체", example = "이미지")
        @RequestParam(required = false) keyword: String?,
        @Parameter(description = "페이지 번호", example = "0")
        @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "페이지 크기", example = "12")
        @RequestParam(defaultValue = "${GetReleaseNoteQueryService.DEFAULT_PAGE_SIZE}") size: Int,
    ): CommonResponse<SliceResponse<ReleaseNoteSummaryResponse>> =
        CommonResponse.success(
            ReleaseNoteResponseCode.RELEASE_NOTE_LIST_SUCCESS,
            getReleaseNoteQueryService.findAll(keyword, page, size),
        )

    @GetMapping("/{releaseNoteId}")
    @Operation(summary = "릴리즈 노트 상세 조회 (발행된 노트만, 비로그인 허용)")
    fun getReleaseNote(
        @PathVariable releaseNoteId: Long,
    ): CommonResponse<ReleaseNoteResponse> =
        CommonResponse.success(
            ReleaseNoteResponseCode.RELEASE_NOTE_DETAIL_SUCCESS,
            getReleaseNoteQueryService.find(releaseNoteId),
        )
}
