package com.weeth.domain.file.domain.vo

import com.weeth.domain.file.application.exception.UnsupportedFileContentTypeException

@JvmInline
value class FileContentType(
    val value: String,
) {
    init {
        if (value !in ALLOWED_CONTENT_TYPES) {
            throw UnsupportedFileContentTypeException()
        }
    }

    companion object {
        private val ALLOWED_CONTENT_TYPES =
            setOf(
                "image/jpeg",
                "image/png",
                "image/webp",
                "application/pdf",
            )
    }
}
