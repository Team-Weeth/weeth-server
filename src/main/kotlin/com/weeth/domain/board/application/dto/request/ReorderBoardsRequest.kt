package com.weeth.domain.board.application.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty

data class ReorderBoardsRequest(
    @field:Schema(description = "원하는 순서대로 정렬된 게시판 ID 목록", example = "[3, 1, 2]")
    @field:NotEmpty
    val boardIds: List<Long>,
)

