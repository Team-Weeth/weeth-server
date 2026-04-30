package com.weeth.domain.comment.domain.vo

@JvmInline
value class CommentContent private constructor(
    val value: String,
) {
    companion object {
        private const val MAX_LENGTH = 300

        fun from(raw: String): CommentContent {
            require(raw.isNotBlank()) { "댓글 내용은 빈 값이 될 수 없습니다." }
            require(raw.length <= MAX_LENGTH) {
                "댓글 내용은 ${MAX_LENGTH}자 이하로 입력해주세요."
            }
            return CommentContent(raw)
        }
    }
}
