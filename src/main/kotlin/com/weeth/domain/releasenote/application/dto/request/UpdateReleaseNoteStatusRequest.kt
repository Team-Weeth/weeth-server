package com.weeth.domain.releasenote.application.dto.request

import com.weeth.domain.releasenote.domain.enums.ReleaseNoteStatus
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

data class UpdateReleaseNoteStatusRequest(
    @field:Schema(
        description = "변경할 발행 상태. PUBLISHED면 publishedAt을 현재 시각으로 기록하고, DRAFT면 비공개 전환하며 publishedAt을 초기화한다.",
        example = "PUBLISHED",
    )
    @field:NotNull
    val status: ReleaseNoteStatus,
)
