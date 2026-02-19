package com.weeth.domain.board.presentation

import com.weeth.domain.board.application.dto.response.BoardDetailResponse
import com.weeth.domain.board.application.dto.response.BoardListResponse
import com.weeth.domain.board.application.exception.BoardErrorCode
import com.weeth.domain.board.application.usecase.query.GetBoardQueryService
import com.weeth.domain.user.domain.entity.enums.Role
import com.weeth.global.auth.annotation.CurrentUserRole
import com.weeth.global.common.exception.ApiErrorCodeExample
import com.weeth.global.common.response.CommonResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "BOARD", description = "게시판 API")
@RestController
@RequestMapping("/api/v4/boards")
@ApiErrorCodeExample(BoardErrorCode::class)
class BoardController(
    private val getBoardQueryService: GetBoardQueryService,
) {
    @GetMapping
    @Operation(summary = "게시판 목록 조회")
    fun findBoards(
        @Parameter(hidden = true) @CurrentUserRole role: Role,
    ): CommonResponse<List<BoardListResponse>> =
        CommonResponse.success(BoardResponseCode.BOARD_FIND_ALL_SUCCESS, getBoardQueryService.findBoards(role))

    @GetMapping("/{boardId}")
    @Operation(summary = "게시판 상세 조회")
    fun findBoard(
        @PathVariable boardId: Long,
        @Parameter(hidden = true) @CurrentUserRole role: Role,
    ): CommonResponse<BoardDetailResponse> =
        CommonResponse.success(
            BoardResponseCode.BOARD_FIND_BY_ID_SUCCESS,
            getBoardQueryService.findBoard(boardId, role),
        )
}
