package com.weeth.domain.board.application.dto.response

import com.weeth.domain.user.domain.entity.enums.Position
import com.weeth.domain.user.domain.entity.enums.Role
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class PostListResponse(
    @field:Schema(description = "게시글 ID")
    val id: Long,
    @field:Schema(description = "작성자명")
    val name: String,
    @field:Schema(description = "작성자 역할")
    val role: Role,
    @field:Schema(description = "제목")
    val title: String,
    @field:Schema(description = "내용")
    val content: String,
    @field:Schema(description = "수정 시각")
    val time: LocalDateTime,
    @field:Schema(description = "댓글 수")
    val commentCount: Int,
    @field:Schema(description = "파일 첨부 여부")
    val hasFile: Boolean,
    @field:Schema(description = "신규 게시글 여부 (24시간 이내)")
    val isNew: Boolean,
)
