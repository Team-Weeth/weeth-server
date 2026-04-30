package com.weeth.domain.file.domain.vo

enum class FileType(
    val contentType: String,
    val extensions: Set<String>,
) {
    JPEG("image/jpeg", setOf("jpg", "jpeg")),
    PNG("image/png", setOf("png")),
    WEBP("image/webp", setOf("webp")),
    PDF("application/pdf", setOf("pdf")),
    ;

    companion object {
        private val BY_CONTENT_TYPE = entries.associateBy { it.contentType }
        private val BY_EXTENSION = entries.flatMap { type -> type.extensions.map { ext -> ext to type } }.toMap()

        /**
         * API 요청의 contentType 검증 시 사용
         * image/png -> FileType.PNG 반환
         * */
        fun fromContentType(contentType: String): FileType? = BY_CONTENT_TYPE[contentType.trim().lowercase()]

        /**
         * 파일명 확장자 검증 시 사용
         * png -> FileType.PNG 반환
         * */
        fun fromExtension(extension: String): FileType? = BY_EXTENSION[extension.trim().lowercase()]
    }
}
