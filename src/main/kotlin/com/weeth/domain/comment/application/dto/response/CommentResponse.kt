package com.weeth.domain.comment.application.dto.response

import com.weeth.domain.file.application.dto.response.FileResponse
import com.weeth.domain.user.domain.entity.enums.Role
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class CommentResponse(
    @field:Schema(description = "댓글 ID", example = "1")
    val id: Long,
    @field:Schema(description = "작성자 이름", example = "홍길동")
    val name: String,
    @field:Schema(description = "작성자 역할", example = "USER")
    val role: Role,
    @field:Schema(description = "댓글 내용", example = "댓글입니다.")
    val content: String,
    @field:Schema(description = "작성 시간", example = "2026-02-18T12:00:00")
    val time: LocalDateTime,
    @field:Schema(description = "첨부 파일 목록")
    val fileUrls: List<FileResponse>,
    @field:Schema(description = "대댓글 목록")
    val children: List<CommentResponse>,
)
