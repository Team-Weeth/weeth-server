package com.weeth.domain.board.application.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class AdminBoardListResponse(
    @field:Schema(description = "게시판 목록 (삭제된 게시판과 가상 전체 게시판 포함)")
    val boards: List<BoardDetailResponse>,
    @field:Schema(description = "현재 활성 게시판 수")
    val activeBoardCount: Int,
    @field:Schema(description = "동아리의 활성 게시판 개수 상한")
    val maxBoardCount: Int,
    @field:Schema(description = "게시판 추가 가능 여부")
    val canCreateBoard: Boolean,
)
