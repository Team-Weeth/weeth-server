package com.weeth.domain.board.application.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty

data class ReorderBoardsRequest(
    @field:Schema(
        description =
            "표시할 순서대로 게시판 ID를 담아 보내주세요. " +
                "공지사항과 전체 게시판은 고정이므로 제외하고 나머지 게시판 ID만 포함해야 합니다.",
        example = "[3, 1, 2]",
    )
    @field:NotEmpty
    val boardIds: List<Long>,
)
