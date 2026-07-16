package com.weeth.domain.board.application.dto.response

import com.weeth.domain.comment.application.dto.response.CommentResponse
import com.weeth.domain.file.application.dto.response.FileResponse
import com.weeth.domain.user.application.dto.response.UserInfo
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class PostDetailResponse(
    @field:Schema(description = "게시글 ID")
    val id: Long,
    @field:Schema(description = "게시판 ID")
    val boardId: Long,
    @field:Schema(description = "게시판 이름")
    val boardName: String,
    @field:Schema(description = "작성자 정보")
    val author: UserInfo,
    @field:Schema(description = "제목")
    val title: String,
    @field:Schema(description = "내용")
    val content: String,
    @field:Schema(description = "생성 시각")
    val time: LocalDateTime,
    @field:Schema(description = "댓글 수")
    val commentCount: Int,
    @field:Schema(description = "좋아요 정보")
    val like: PostLikeResponse,
    @field:Schema(description = "댓글 목록")
    val comments: List<CommentResponse>,
    @field:Schema(description = "첨부 파일 목록")
    val fileUrls: List<FileResponse>,
    @field:Schema(description = "신규 게시글 여부 (24시간 이내)")
    val isNew: Boolean,
    @field:Schema(description = "게시판 설정")
    val boardConfig: BoardConfigResponse,
)
