package com.weeth.domain.comment.application.dto.request

import com.weeth.domain.file.application.dto.request.FileSaveRequest
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class CommentUpdateRequest(
    @field:NotBlank
    @field:Size(max = 300, message = "댓글은 최대 300자까지 가능합니다.")
    val content: String,
    @field:Schema(
        description = "첨부 파일 변경 규약: null=변경 안 함, []=전체 삭제, 배열 전달=해당 목록으로 교체",
        nullable = true,
    )
    @field:Valid
    val files: List<@NotNull FileSaveRequest>? = null,
)
