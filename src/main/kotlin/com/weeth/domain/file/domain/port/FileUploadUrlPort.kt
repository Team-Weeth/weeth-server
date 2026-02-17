package com.weeth.domain.file.domain.port

import com.weeth.domain.file.domain.entity.FileOwnerType

data class FileUploadUrl(
    val fileName: String,
    val storageKey: String,
    val url: String,
)

interface FileUploadUrlPort {
    fun generateUploadUrl(
        ownerType: FileOwnerType,
        fileName: String,
    ): FileUploadUrl
}
