package com.weeth.domain.releasenote.application.dto.response

import com.weeth.domain.releasenote.domain.enums.ReleaseNoteStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

/**
 * 목록 카드용 요약 응답. 본문([content])은 제외하고 서버가 본문에서 추출한 [summary]만 포함한다.
 */
data class ReleaseNoteSummaryResponse(
    @field:Schema(description = "릴리즈 노트 ID", example = "12")
    val id: Long,
    @field:Schema(description = "제목", example = "게시판 이미지 표시 개선")
    val title: String,
    @field:Schema(description = "본문에서 추출한 요약(최대 100자)", example = "본문에 넣은 이미지가 첨부 목록에 중복으로 보이지 않아요.")
    val summary: String,
    @field:Schema(description = "카드 썸네일 이미지 URL", example = "https://...", nullable = true)
    val thumbnailUrl: String? = null,
    @field:Schema(description = "발행 상태", example = "PUBLISHED")
    val status: ReleaseNoteStatus,
    @field:Schema(description = "서비스에 실제 반영된 날짜(업데이트 일자)", example = "2026-11-02T00:00:00", nullable = true)
    val releasedAt: LocalDateTime? = null,
    @field:Schema(description = "발행된 시각. DRAFT는 null.", example = "2026-11-01T18:00:00", nullable = true)
    val publishedAt: LocalDateTime? = null,
)
