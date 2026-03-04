package com.weeth.domain.comment.application.exception

import com.weeth.global.common.exception.ErrorCodeInterface
import com.weeth.global.common.exception.ExplainError
import org.springframework.http.HttpStatus

enum class CommentErrorCode(
    override val code: Int,
    override val status: HttpStatus,
    override val message: String,
) : ErrorCodeInterface {
    @ExplainError("요청한 댓글 ID에 해당하는 댓글이 존재하지 않을 때 발생합니다.")
    COMMENT_NOT_FOUND(20500, HttpStatus.NOT_FOUND, "존재하지 않는 댓글입니다."),

    @ExplainError("댓글 작성자가 아닌 사용자가 수정/삭제를 시도할 때 발생합니다.")
    COMMENT_NOT_OWNED(20501, HttpStatus.FORBIDDEN, "댓글 작성자만 수정/삭제할 수 있습니다."),

    @ExplainError("이미 삭제된 댓글에 대해 삭제를 재시도할 때 발생합니다.")
    COMMENT_ALREADY_DELETED(20502, HttpStatus.BAD_REQUEST, "이미 삭제된 댓글입니다."),
}
