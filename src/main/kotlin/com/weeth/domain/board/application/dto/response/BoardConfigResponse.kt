package com.weeth.domain.board.application.dto.response

import com.weeth.domain.board.domain.entity.Board
import com.weeth.domain.club.domain.enums.MemberRole
import io.swagger.v3.oas.annotations.media.Schema

data class BoardConfigResponse(
    @field:Schema(description = "게시판 읽기 가능 여부")
    val canRead: Boolean,
    @field:Schema(description = "글 작성 가능 여부")
    val canWrite: Boolean,
    @field:Schema(description = "댓글 작성 가능 여부")
    val canComment: Boolean,
) {
    companion object {
        fun of(
            board: Board,
            memberRole: MemberRole,
        ): BoardConfigResponse {
            val canRead = board.isAccessibleBy(memberRole)

            return BoardConfigResponse(
                canRead = canRead,
                canWrite = board.canWriteBy(memberRole),
                canComment = canRead && board.isCommentEnabled,
            )
        }
    }
}
