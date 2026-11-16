package com.weeth.domain.releasenote.application.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

/**
 * 릴리즈 노트 수정 요청(PUT, 전체 교체). 발행 상태([status])는 바꾸지 않으며,
 * 상태 변경은 전용 PATCH 엔드포인트를 사용한다.
 */
data class UpdateReleaseNoteRequest(
    @field:Schema(description = "제목", example = "게시판 이미지 표시 개선")
    @field:NotBlank
    @field:Size(max = 100)
    val title: String,
    @field:Schema(description = "본문(tiptap HTML)", example = "<h3>개선된 기능 : ...</h3><p>...</p>")
    @field:NotBlank
    val content: String,
    @field:Schema(description = "카드 썸네일 이미지 URL", example = "https://...", nullable = true)
    val thumbnailUrl: String? = null,
    @field:Schema(description = "서비스에 실제 반영된 날짜(업데이트 일자)", example = "2026-11-02T00:00:00", nullable = true)
    val releasedAt: LocalDateTime? = null,
)
