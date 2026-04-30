package com.weeth.domain.board.application.dto.request

import com.weeth.domain.file.application.dto.request.FileSaveRequest
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class UpdatePostRequest(
    @field:Schema(description = "게시글 제목 (null=변경 안 함)")
    @field:Size(max = 200)
    val title: String? = null,
    @field:Schema(description = "게시글 내용 (null=변경 안 함)")
    val content: String? = null,
    @field:Schema(description = "첨부 파일 변경 규약: null=변경 안 함, []=전체 삭제, 배열 전달=해당 목록으로 교체", nullable = true)
    @field:Valid
    val files: List<@NotNull FileSaveRequest>? = null,
)
