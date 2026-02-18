package com.weeth.domain.file.domain.vo

import com.weeth.domain.file.application.exception.UnsupportedFileContentTypeException

@JvmInline
value class FileContentType(
    val value: String,
) {
    val normalized: String
        get() = value.lowercase()

    val fileType: FileType
        get() = FileType.fromContentType(normalized) ?: throw UnsupportedFileContentTypeException()

    init {
        if (FileType.fromContentType(normalized) == null) {
            throw UnsupportedFileContentTypeException()
        }
    }
}
