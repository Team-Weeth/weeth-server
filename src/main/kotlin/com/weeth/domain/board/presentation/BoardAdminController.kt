package com.weeth.domain.board.presentation

import com.weeth.domain.board.application.dto.request.CreateBoardRequest
import com.weeth.domain.board.application.dto.request.UpdateBoardRequest
import com.weeth.domain.board.application.dto.response.BoardDetailResponse
import com.weeth.domain.board.application.exception.BoardErrorCode
import com.weeth.domain.board.application.usecase.command.ManageBoardUseCase
import com.weeth.domain.board.application.usecase.query.GetBoardQueryService
import com.weeth.global.auth.annotation.CurrentUser
import com.weeth.global.common.exception.ApiErrorCodeExample
import com.weeth.global.common.response.CommonResponse
import com.weeth.global.common.web.TsidParam
import com.weeth.global.common.web.TsidPathVariable
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
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
        @TsidParam
        @TsidPathVariable clubId: Long,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<List<BoardDetailResponse>> =
        CommonResponse.success(
            BoardResponseCode.BOARD_FIND_ALL_SUCCESS,
            getBoardQueryService.findAllBoardsForAdmin(clubId, userId),
        )

    @GetMapping("/{boardId}")
    @Operation(summary = "게시판 상세 조회 (삭제된 게시판 포함)")
    fun findBoard(
        @TsidParam
        @TsidPathVariable clubId: Long,
        @PathVariable boardId: Long,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<BoardDetailResponse> =
        CommonResponse.success(
            BoardResponseCode.BOARD_FIND_BY_ID_SUCCESS,
            getBoardQueryService.findBoardDetailForAdmin(clubId, userId, boardId),
        )

    @PostMapping
    @Operation(summary = "게시판 생성")
    fun createBoard(
        @TsidParam
        @TsidPathVariable clubId: Long,
        @RequestBody @Valid request: CreateBoardRequest,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<BoardDetailResponse> =
        CommonResponse.success(
            BoardResponseCode.BOARD_CREATED_SUCCESS,
            manageBoardUseCase.create(clubId, request, userId),
        )

    @PatchMapping("/{boardId}")
    @Operation(summary = "게시판 설정/이름 수정")
    fun updateBoard(
        @TsidParam
        @TsidPathVariable clubId: Long,
        @PathVariable boardId: Long,
        @RequestBody @Valid request: UpdateBoardRequest,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<BoardDetailResponse> =
        CommonResponse.success(
            BoardResponseCode.BOARD_UPDATED_SUCCESS,
            manageBoardUseCase.update(clubId, boardId, request, userId),
        )

    @DeleteMapping("/{boardId}")
    @Operation(summary = "게시판 삭제")
    fun deleteBoard(
        @TsidParam
        @TsidPathVariable clubId: Long,
        @PathVariable boardId: Long,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<Void?> {
        manageBoardUseCase.delete(clubId, boardId, userId)
        return CommonResponse.success(BoardResponseCode.BOARD_DELETED_SUCCESS)
    }
}
