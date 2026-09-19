package com.weeth.domain.file.domain.vo

enum class FileType(
    val contentType: String,
    val extensions: Set<String>,
) {
    JPEG("image/jpeg", setOf("jpg", "jpeg")),
    PNG("image/png", setOf("png")),
    WEBP("image/webp", setOf("webp")),
    PDF("application/pdf", setOf("pdf")),

    // V3 마이그레이션 대상에 세션 자료(pptx 27건)와 문서(docx 2건)가 포함되어 추가했다.
    // svg는 XML이라 <script>를 품을 수 있어 인라인 서빙 시 XSS 벡터가 되므로 허용하지 않는다.
    PPTX(
        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        setOf("pptx"),
    ),
    DOCX(
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        setOf("docx"),
    ),
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
