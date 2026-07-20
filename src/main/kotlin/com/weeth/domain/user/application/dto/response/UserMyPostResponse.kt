package com.weeth.domain.user.application.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class UserMyPostResponse(
    @field:Schema(description = "게시글 ID", example = "1")
    val postId: Long,
    @field:Schema(description = "동아리 ID", example = "1A2b3C")
    val clubId: String,
    @field:Schema(description = "동아리 이름", example = "Leets")
    val clubName: String,
    @field:Schema(description = "게시판 ID", example = "10")
    val boardId: Long,
    @field:Schema(description = "게시판 이름", example = "자유게시판")
    val boardName: String,
    @field:Schema(description = "게시글 제목", example = "제목")
    val title: String,
    @field:Schema(description = "게시글 내용", example = "내용")
    val content: String,
    @field:Schema(description = "댓글 수", example = "3")
    val commentCount: Int,
    @field:Schema(description = "좋아요 수", example = "5")
    val likeCount: Int,
    @field:Schema(description = "작성 시각")
    val createdAt: LocalDateTime,
    @field:Schema(description = "신규 게시글 여부 (24시간 이내)", example = "true")
    val isNew: Boolean,
)
