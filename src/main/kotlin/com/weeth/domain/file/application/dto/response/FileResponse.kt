package com.weeth.domain.file.application.dto.response

import com.weeth.domain.file.domain.entity.FileStatus
import io.swagger.v3.oas.annotations.media.Schema

data class FileResponse(
    @field:Schema(description = "파일 ID", example = "1")
    val fileId: Long,
    @field:Schema(description = "원본 파일명", example = "profile-image.png")
    val fileName: String,
    @field:Schema(
        description = "조회용 파일 URL",
        example = "https://bucket.s3.ap-northeast-2.amazonaws.com/POST/2026-02/uuid_profile-image.png",
    )
    val fileUrl: String,
    @field:Schema(description = "저장소 키", example = "POST/2026-02/uuid_profile-image.png")
    val storageKey: String,
    @field:Schema(description = "파일 크기(bytes)", example = "102400")
    val fileSize: Long,
    @field:Schema(description = "파일 Content-Type", example = "image/png")
    val contentType: String,
    @field:Schema(description = "파일 상태", example = "UPLOADED")
    val status: FileStatus,
)
