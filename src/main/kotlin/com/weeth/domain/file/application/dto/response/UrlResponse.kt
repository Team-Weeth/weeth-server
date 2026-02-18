package com.weeth.domain.file.application.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class UrlResponse(
    @field:Schema(description = "원본 파일명", example = "profile-image.png")
    val fileName: String,
    @field:Schema(description = "Presigned PUT URL", example = "https://bucket.s3.amazonaws.com/TEMP/2026-02/uuid_profile-image.png")
    val putUrl: String,
    @field:Schema(description = "저장소 키", example = "TEMP/2026-02/uuid_profile-image.png")
    val storageKey: String,
)
