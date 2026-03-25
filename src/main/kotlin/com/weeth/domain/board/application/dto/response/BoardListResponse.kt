package com.weeth.domain.board.application.dto.response

import com.weeth.domain.board.domain.enums.BoardType
import io.swagger.v3.oas.annotations.media.Schema

data class BoardListResponse(
    @field:Schema(description = "게시판 ID (전체 게시판은 null)")
    val id: Long?,
    @field:Schema(description = "게시판 이름")
    val name: String,
    @field:Schema(description = "게시판 타입")
    val type: BoardType,
)
