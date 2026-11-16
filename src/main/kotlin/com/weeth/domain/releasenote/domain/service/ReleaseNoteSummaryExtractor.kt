package com.weeth.domain.releasenote.domain.service

/**
 * 릴리즈 노트 본문(tiptap HTML)에서 목록 카드용 요약 텍스트를 뽑아낸다.
 * 외부 HTML 파서 의존성 없이 태그를 제거하고 공백을 정리한 뒤 앞 [MAX_SUMMARY_LENGTH]자만 사용한다.
 * 작성자가 신뢰된 DEVELOPER뿐이라 sanitize 목적이 아니라 순수 요약 목적이다.
 */
object ReleaseNoteSummaryExtractor {
    const val MAX_SUMMARY_LENGTH = 100

    private val SCRIPT_OR_STYLE = Regex("(?is)<(script|style)[^>]*>.*?</\\1>")
    private val HTML_TAG = Regex("<[^>]*>")
    private val WHITESPACE = Regex("\\s+")

    private val ENTITIES =
        mapOf(
            "&nbsp;" to " ",
            "&amp;" to "&",
            "&lt;" to "<",
            "&gt;" to ">",
            "&quot;" to "\"",
            "&#39;" to "'",
            "&apos;" to "'",
        )

    fun extract(content: String): String {
        var text = SCRIPT_OR_STYLE.replace(content, " ")
        text = HTML_TAG.replace(text, " ")
        ENTITIES.forEach { (entity, replacement) -> text = text.replace(entity, replacement) }
        text = WHITESPACE.replace(text, " ").trim()
        return text.take(MAX_SUMMARY_LENGTH)
    }
}
