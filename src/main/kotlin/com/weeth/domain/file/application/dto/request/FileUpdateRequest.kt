package com.weeth.domain.file.application.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

data class FileUpdateRequest(
    @field:Schema(description = "파일 ID", example = "1")
    @field:NotNull
    val fileId: Long,
    @field:Schema(description = "원본 파일명", example = "receipt-2026-02.pdf")
    @field:NotBlank
    val fileName: String,
    @field:Schema(description = "저장소 키", example = "RECEIPT/2026-02/550e8400-e29b-41d4-a716-446655440000_receipt-2026-02.pdf")
    @field:NotBlank
    val storageKey: String,
    @field:Schema(description = "파일 크기(bytes)", example = "204800")
    @field:Positive
    val fileSize: Long,
    @field:Schema(description = "파일 Content-Type", example = "application/pdf")
    @field:NotBlank
    val contentType: String,
)
