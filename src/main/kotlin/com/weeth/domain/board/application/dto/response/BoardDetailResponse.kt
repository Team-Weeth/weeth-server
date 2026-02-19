package com.weeth.domain.board.application.dto.response

import com.fasterxml.jackson.annotation.JsonInclude
import com.weeth.domain.board.domain.entity.enums.BoardType
import com.weeth.domain.user.domain.entity.enums.Role
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
data class BoardDetailResponse(
    @field:Schema(description = "게시판 ID")
    val id: Long,
    @field:Schema(description = "게시판 이름")
    val name: String,
    @field:Schema(description = "게시판 타입")
    val type: BoardType,
    @field:Schema(description = "댓글 허용 여부")
    val commentEnabled: Boolean,
    @field:Schema(description = "게시글 작성 권한")
    val writePermission: Role,
    @field:Schema(description = "비공개 게시판 여부")
    val isPrivate: Boolean,
    @field:Schema(description = "삭제 여부 (관리자 페이지에서만 값 존재)")
    val isDeleted: Boolean?,
)
