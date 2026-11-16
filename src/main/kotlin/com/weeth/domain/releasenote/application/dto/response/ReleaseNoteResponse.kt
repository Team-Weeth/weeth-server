package com.weeth.domain.releasenote.application.dto.response

import com.weeth.domain.releasenote.domain.enums.ReleaseNoteStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class ReleaseNoteResponse(
    @field:Schema(description = "릴리즈 노트 ID", example = "12")
    val id: Long,
    @field:Schema(description = "제목", example = "게시판 이미지 표시 개선")
    val title: String,
    @field:Schema(description = "카드 썸네일 이미지 URL", example = "https://...", nullable = true)
    val thumbnailUrl: String? = null,
    @field:Schema(description = "본문(tiptap HTML)", example = "<h3>...</h3>")
    val content: String,
    @field:Schema(description = "발행 상태", example = "PUBLISHED")
    val status: ReleaseNoteStatus,
    @field:Schema(description = "서비스에 실제 반영된 날짜(업데이트 일자)", example = "2026-11-02T00:00:00", nullable = true)
    val releasedAt: LocalDateTime? = null,
    @field:Schema(description = "발행된 시각. DRAFT는 null.", example = "2026-11-01T18:00:00", nullable = true)
    val publishedAt: LocalDateTime? = null,
    @field:Schema(description = "생성 시각", example = "2026-11-01T10:00:00")
    val createdAt: LocalDateTime,
    @field:Schema(description = "수정 시각", example = "2026-11-01T18:00:00")
    val updatedAt: LocalDateTime,
    @field:Schema(description = "작성자 유저 ID", example = "1")
    val authorId: Long,
)
