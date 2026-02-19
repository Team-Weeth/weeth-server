package com.weeth.domain.board.application.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class PostSaveResponse(
    @field:Schema(description = "게시글 ID", example = "1")
    val id: Long,
)
