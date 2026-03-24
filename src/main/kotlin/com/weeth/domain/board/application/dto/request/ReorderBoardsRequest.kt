package com.weeth.domain.board.application.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty

data class ReorderBoardsRequest(
    @field:Schema(
        description =
            "표시할 순서대로 게시판 ID를 담아 보내주세요. " +
                "배열의 첫 번째 항목이 1번째 게시판으로 표시됩니다.",
        example = "[3, 1, 2]",
    )
    @field:NotEmpty
    val boardIds: List<Long>,
)
