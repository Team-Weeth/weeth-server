package com.weeth.domain.board.presentation

import com.weeth.domain.board.application.dto.response.BoardListResponse
import com.weeth.domain.board.application.exception.BoardErrorCode
import com.weeth.domain.board.application.usecase.query.GetBoardQueryService
import com.weeth.domain.user.domain.enums.Role
import com.weeth.global.auth.annotation.CurrentUser
import com.weeth.global.auth.annotation.CurrentUserRole
import com.weeth.global.common.exception.ApiErrorCodeExample
import com.weeth.global.common.response.CommonResponse
import com.weeth.global.common.web.TsidParam
import com.weeth.global.common.web.TsidPathVariable
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "BOARD", description = "게시판 API")
@RestController
@RequestMapping("/api/v4/clubs/{clubId}/boards")
@ApiErrorCodeExample(BoardErrorCode::class)
class BoardController(
    private val getBoardQueryService: GetBoardQueryService,
) {
    @GetMapping
    @Operation(summary = "게시판 목록 조회")
    fun findBoards(
        @TsidParam
        @TsidPathVariable clubId: Long,
        @Parameter(hidden = true) @CurrentUser userId: Long,
        @Parameter(hidden = true) @CurrentUserRole role: Role, // TODO: 멀티 테넨시 지원으로 Jwt에 포함한 Role은 삭제 예정
    ): CommonResponse<List<BoardListResponse>> =
        CommonResponse.success(
            BoardResponseCode.BOARD_FIND_ALL_SUCCESS,
            getBoardQueryService.findBoards(clubId, userId, role),
        )
}
