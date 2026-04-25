package com.weeth.domain.dashboard.application.dto.response

import com.weeth.domain.board.application.dto.response.PostLikeResponse
import com.weeth.domain.file.application.dto.response.FileResponse
import com.weeth.domain.user.application.dto.response.UserInfo
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class DashboardPostResponse(
    @field:Schema(description = "게시글 ID", example = "1")
    val id: Long,
    @field:Schema(description = "게시판 ID", example = "1")
    val boardId: Long,
    @field:Schema(description = "작성자 정보")
    val author: UserInfo,
    @field:Schema(description = "제목", example = "안녕하세요")
    val title: String,
    @field:Schema(description = "내용", example = "오늘은 날씨가 좋네요")
    val content: String,
    @field:Schema(description = "작성일")
    val time: LocalDateTime,
    @field:Schema(description = "댓글 수", example = "5")
    val commentCount: Int,
    @field:Schema(description = "좋아요 정보")
    val like: PostLikeResponse,
    @field:Schema(description = "첨부 파일 목록")
    val fileUrls: List<FileResponse>,
    @field:Schema(description = "24시간 내 새 게시글 여부", example = "true")
    val isNew: Boolean,
)
