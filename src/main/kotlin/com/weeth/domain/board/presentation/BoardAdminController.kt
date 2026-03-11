package com.weeth.domain.board.presentation

import com.weeth.domain.board.application.dto.request.CreateBoardRequest
import com.weeth.domain.board.application.dto.request.UpdateBoardRequest
import com.weeth.domain.board.application.dto.response.BoardDetailResponse
import com.weeth.domain.board.application.exception.BoardErrorCode
import com.weeth.domain.board.application.usecase.command.ManageBoardUseCase
import com.weeth.domain.board.application.usecase.query.GetBoardQueryService
import com.weeth.global.common.exception.ApiErrorCodeExample
import com.weeth.global.common.response.CommonResponse
import com.weeth.global.common.web.TsidParam
import com.weeth.global.common.web.TsidPathVariable
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
@RequestMapping("/api/v4/admin/clubs/{clubId}/boards")
@PreAuthorize("hasRole('ADMIN')")
@ApiErrorCodeExample(BoardErrorCode::class)
class BoardAdminController(
    private val manageBoardUseCase: ManageBoardUseCase,
    private val getBoardQueryService: GetBoardQueryService,
) {
    @GetMapping
    @Operation(summary = "게시판 전체 목록 조회 (삭제/비공개 포함)")
    fun findAllBoards(
        @PathVariable @TsidParam
        @TsidPathVariable clubId: Long,
    ): CommonResponse<List<BoardDetailResponse>> =
        CommonResponse.success(
            BoardResponseCode.BOARD_FIND_ALL_SUCCESS,
            getBoardQueryService.findAllBoardsForAdmin(clubId),
        )

    @GetMapping("/{boardId}")
    @Operation(summary = "게시판 상세 조회 (삭제된 게시판 포함)")
    fun findBoard(
        @PathVariable @TsidParam
        @TsidPathVariable clubId: Long,
        @PathVariable boardId: Long,
    ): CommonResponse<BoardDetailResponse> =
        CommonResponse.success(
            BoardResponseCode.BOARD_FIND_BY_ID_SUCCESS,
            getBoardQueryService.findBoardDetailForAdmin(clubId, boardId),
        )

    @PostMapping
    @Operation(summary = "게시판 생성")
    fun createBoard(
        @PathVariable @TsidParam
        @TsidPathVariable clubId: Long,
        @RequestBody @Valid request: CreateBoardRequest,
    ): CommonResponse<BoardDetailResponse> =
        CommonResponse.success(BoardResponseCode.BOARD_CREATED_SUCCESS, manageBoardUseCase.create(clubId, request))

    @PatchMapping("/{boardId}")
    @Operation(summary = "게시판 설정/이름 수정")
    fun updateBoard(
        @PathVariable @TsidParam
        @TsidPathVariable clubId: Long,
        @PathVariable boardId: Long,
        @RequestBody @Valid request: UpdateBoardRequest,
    ): CommonResponse<BoardDetailResponse> =
        CommonResponse.success(
            BoardResponseCode.BOARD_UPDATED_SUCCESS,
            manageBoardUseCase.update(clubId, boardId, request),
        )

    @DeleteMapping("/{boardId}")
    @Operation(summary = "게시판 삭제")
    fun deleteBoard(
        @PathVariable @TsidParam
        @TsidPathVariable clubId: Long,
        @PathVariable boardId: Long,
    ): CommonResponse<Void?> {
        manageBoardUseCase.delete(clubId, boardId)
        return CommonResponse.success(BoardResponseCode.BOARD_DELETED_SUCCESS)
    }
}
