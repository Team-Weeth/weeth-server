package com.weeth.domain.releasenote.application.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class ReleaseNoteIdResponse(
    @field:Schema(description = "생성된 릴리즈 노트 ID", example = "12")
    val id: Long,
)
