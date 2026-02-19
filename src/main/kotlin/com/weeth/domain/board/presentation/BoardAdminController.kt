package com.weeth.domain.board.presentation

import com.weeth.domain.board.application.dto.request.CreateBoardRequest
import com.weeth.domain.board.application.dto.request.UpdateBoardRequest
import com.weeth.domain.board.application.dto.response.BoardDetailResponse
import com.weeth.domain.board.application.exception.BoardErrorCode
import com.weeth.domain.board.application.usecase.command.ManageBoardUseCase
import com.weeth.domain.board.application.usecase.query.GetBoardQueryService
import com.weeth.global.common.exception.ApiErrorCodeExample
import com.weeth.global.common.response.CommonResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Board-Admin", description = "Board Admin API")
@RestController
@RequestMapping("/api/v4/admin/board")
@PreAuthorize("hasRole('ADMIN')")
@ApiErrorCodeExample(BoardErrorCode::class)
class BoardAdminController(
    private val manageBoardUseCase: ManageBoardUseCase,
    private val getBoardQueryService: GetBoardQueryService,
) {
    @GetMapping
    @Operation(summary = "게시판 전체 목록 조회 (삭제/비공개 포함)")
    fun findAllBoards(): CommonResponse<List<BoardDetailResponse>> =
        CommonResponse.success(BoardResponseCode.BOARD_FIND_ALL_SUCCESS, getBoardQueryService.findAllBoardsForAdmin())

    @PostMapping
    @Operation(summary = "게시판 생성")
    fun createBoard(
        @RequestBody @Valid request: CreateBoardRequest,
    ): CommonResponse<BoardDetailResponse> =
        CommonResponse.success(BoardResponseCode.BOARD_CREATED_SUCCESS, manageBoardUseCase.create(request))

    @PatchMapping("/{boardId}")
    @Operation(summary = "게시판 설정/이름 수정")
    fun updateBoard(
        @PathVariable boardId: Long,
        @RequestBody @Valid request: UpdateBoardRequest,
    ): CommonResponse<BoardDetailResponse> =
        CommonResponse.success(BoardResponseCode.BOARD_UPDATED_SUCCESS, manageBoardUseCase.update(boardId, request))

    @DeleteMapping("/{boardId}")
    @Operation(summary = "게시판 삭제")
    fun deleteBoard(
        @PathVariable boardId: Long,
    ): CommonResponse<Void?> {
        manageBoardUseCase.delete(boardId)
        return CommonResponse.success(BoardResponseCode.BOARD_DELETED_SUCCESS)
    }
}
