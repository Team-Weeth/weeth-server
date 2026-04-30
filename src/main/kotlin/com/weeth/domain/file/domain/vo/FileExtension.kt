package com.weeth.domain.file.domain.vo

import com.weeth.domain.file.application.exception.UnsupportedFileExtensionException

class FileExtension(
    value: String,
) {
    val normalized: String = value.lowercase()
    val fileType: FileType

    init {
        val resolvedType = FileType.fromExtension(normalized) ?: throw UnsupportedFileExtensionException()
        fileType = resolvedType
    }
}
