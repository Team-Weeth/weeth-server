package com.weeth.domain.board.application.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class BoardNameDuplicateResponse(
    @field:Schema(description = "게시판 이름 중복 여부", example = "true")
    val duplicated: Boolean,
)
