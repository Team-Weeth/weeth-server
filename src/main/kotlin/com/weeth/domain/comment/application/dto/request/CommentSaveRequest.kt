package com.weeth.domain.comment.application.dto.request

import com.weeth.domain.file.application.dto.request.FileSaveRequest
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class CommentSaveRequest(
    val parentCommentId: Long? = null,
    @field:NotBlank
    @field:Size(max = 300, message = "댓글은 최대 300자까지 가능합니다.")
    val content: String,
    @field:Valid
    val files: List<@NotNull FileSaveRequest>? = null,
)
