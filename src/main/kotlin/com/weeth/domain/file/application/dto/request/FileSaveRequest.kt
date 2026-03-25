package com.weeth.domain.file.application.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive

data class FileSaveRequest(
    @field:Schema(description = "원본 파일명", example = "profile-image.png")
    @field:NotBlank
    val fileName: String,
    @field:Schema(
        description = "저장소 키. `Type/YY-MM/UUID_원본파일명` 형식",
        example = "POST/2026-02/2c0a4d45-ec94-4ec0-85e1-b489c2eaf9c3_profile-image.png",
    )
    @field:NotBlank
    val storageKey: String,
    @field:Schema(description = "파일 크기(bytes)", example = "102400")
    @field:Positive
    val fileSize: Long,
    @field:Schema(description = "파일 Content-Type. `image/png, image/jpeg, application/pdf` 지원", example = "image/png")
    @field:NotBlank
    val contentType: String,
)
