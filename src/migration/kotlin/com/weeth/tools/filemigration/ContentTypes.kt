package com.weeth.tools.filemigration

/**
 * V4 `FileType` 허용 목록과 일치시킨다.
 *
 * S3 HeadObject가 돌려주는 ContentType은 신뢰하지 않는다. V3는 presigned PUT으로
 * 업로드했고 클라이언트가 Content-Type을 지정하지 않으면 `binary/octet-stream`이
 * 저장되기 때문이다. 확장자 기반 추론을 우선하고, 실패 시에만 HeadObject 값을 쓴다.
 *
 * pptx/docx/mp4를 허용하기로 결정하면 [SUPPORTED]에 추가한다.
 * svg는 XML이라 `<script>`를 품을 수 있어 인라인 서빙 시 XSS 벡터가 된다. 추가하지 않는다.
 */
object ContentTypes {
    private val SUPPORTED =
        mapOf(
            "jpg" to "image/jpeg",
            "jpeg" to "image/jpeg",
            "png" to "image/png",
            "webp" to "image/webp",
            "pdf" to "application/pdf",
        )

    fun fromExtension(extension: String?): String? = extension?.lowercase()?.let { SUPPORTED[it] }

    fun isSupported(contentType: String?): Boolean = contentType != null && contentType.lowercase() in SUPPORTED.values
}
