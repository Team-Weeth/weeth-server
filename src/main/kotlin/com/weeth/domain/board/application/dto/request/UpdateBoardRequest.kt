package com.weeth.domain.board.application.dto.request

import com.weeth.domain.user.domain.entity.enums.Role
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

data class UpdateBoardRequest(
    @field:Schema(description = "게시판 이름", example = "새 공지사항", nullable = true)
    @field:Size(max = 100)
    val name: String? = null,
    @field:Schema(description = "댓글 허용 여부", example = "true", nullable = true)
    val commentEnabled: Boolean? = null,
    @field:Schema(description = "게시글 작성 권한", example = "USER", nullable = true)
    val writePermission: Role? = null,
    @field:Schema(description = "비공개 게시판 여부", example = "false", nullable = true)
    val isPrivate: Boolean? = null,
)
