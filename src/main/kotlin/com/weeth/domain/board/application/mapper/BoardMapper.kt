package com.weeth.domain.board.application.mapper

import com.weeth.domain.board.application.dto.response.BoardDetailResponse
import com.weeth.domain.board.application.dto.response.BoardListResponse
import com.weeth.domain.board.domain.entity.Board
import org.springframework.stereotype.Component

@Component
class BoardMapper {
    fun toListResponse(board: Board) =
        BoardListResponse(
            id = board.id,
            name = board.name,
            type = board.type,
        )

    fun toDetailResponse(board: Board) =
        BoardDetailResponse(
            id = board.id,
            name = board.name,
            type = board.type,
            commentEnabled = board.config.commentEnabled,
            writePermission = board.config.writePermission,
            isPrivate = board.config.isPrivate,
            isDeleted = null, // public api에서 삭제 여부는 보여주지 않음
        )

    fun toDetailResponseForAdmin(board: Board) =
        BoardDetailResponse(
            id = board.id,
            name = board.name,
            type = board.type,
            commentEnabled = board.config.commentEnabled,
            writePermission = board.config.writePermission,
            isPrivate = board.config.isPrivate,
            isDeleted = board.isDeleted,
        )
}
