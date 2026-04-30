package com.weeth.domain.comment.application.dto.request

import com.weeth.domain.file.application.dto.request.FileSaveRequest
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class CommentSaveRequest(
    @field:Schema(description = "부모 댓글 ID (대댓글인 경우)", example = "1", nullable = true)
    val parentCommentId: Long? = null,
    @field:Schema(description = "댓글 내용", example = "댓글입니다.")
    @field:NotBlank
    @field:Size(max = 300, message = "댓글은 최대 300자까지 가능합니다.")
    val content: String,
    @field:Schema(description = "첨부 파일 목록", nullable = true)
    @field:Valid
    val files: List<@NotNull FileSaveRequest>? = null,
)
