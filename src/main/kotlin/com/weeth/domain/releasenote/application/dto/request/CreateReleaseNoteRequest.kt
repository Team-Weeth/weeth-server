package com.weeth.domain.releasenote.application.dto.request

import com.weeth.domain.releasenote.domain.enums.ReleaseNoteStatus
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

data class CreateReleaseNoteRequest(
    @field:Schema(description = "제목. 카드/목록 부제로 쓰이므로 접두어는 붙이지 않는다.", example = "게시판 이미지 표시 개선")
    @field:NotBlank
    @field:Size(max = 100)
    val title: String,
    @field:Schema(description = "본문(tiptap HTML)", example = "<h3>개선된 기능 : ...</h3><p>...</p>")
    @field:NotBlank
    val content: String,
    @field:Schema(description = "카드 썸네일 이미지 URL. 없으면 기본 그라데이션 노출.", example = "https://...", nullable = true)
    val thumbnailUrl: String? = null,
    @field:Schema(description = "서비스에 실제 반영된 날짜(업데이트 일자)", example = "2026-11-02T00:00:00", nullable = true)
    val releasedAt: LocalDateTime? = null,
    @field:Schema(
        description = "발행 상태. 기본값 DRAFT. PUBLISHED로 보내면 즉시 발행된다.",
        example = "DRAFT",
    )
    val status: ReleaseNoteStatus = ReleaseNoteStatus.DRAFT,
)
