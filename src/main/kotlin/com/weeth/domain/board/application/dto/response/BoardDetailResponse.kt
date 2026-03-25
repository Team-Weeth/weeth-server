package com.weeth.domain.board.application.dto.response

import com.fasterxml.jackson.annotation.JsonInclude
import com.weeth.domain.board.domain.enums.BoardType
import com.weeth.domain.club.domain.enums.MemberRole
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
data class BoardDetailResponse(
    @field:Schema(description = "게시판 ID (전체 게시판은 null)")
    val id: Long?,
    @field:Schema(description = "게시판 이름")
    val name: String,
    @field:Schema(description = "게시판 타입")
    val type: BoardType,
    @field:Schema(description = "댓글 허용 여부 (전체 게시판은 null)")
    val commentEnabled: Boolean?,
    @field:Schema(description = "게시글 작성 권한 (전체 게시판은 null)")
    val writePermission: MemberRole?,
    @field:Schema(description = "비공개 게시판 여부 (전체 게시판은 null)")
    val isPrivate: Boolean?,
    @field:Schema(description = "표시 순서 (전체 게시판은 null)")
    val displayOrder: Int?,
    @field:Schema(description = "게시글 수 (관리자 페이지에서만 값 존재)")
    val postCount: Int? = null,
    @field:Schema(description = "삭제 여부 (관리자 페이지에서만 값 존재)")
    val isDeleted: Boolean?,
)
