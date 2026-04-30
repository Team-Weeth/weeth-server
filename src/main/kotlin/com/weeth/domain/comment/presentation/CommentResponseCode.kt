package com.weeth.domain.comment.presentation

import com.weeth.global.common.response.ResponseCodeInterface
import org.springframework.http.HttpStatus

enum class CommentResponseCode(
    override val code: Int,
    override val status: HttpStatus,
    override val message: String,
) : ResponseCodeInterface {
    POST_COMMENT_CREATED_SUCCESS(10500, HttpStatus.OK, "게시글 댓글이 성공적으로 생성되었습니다."),
    POST_COMMENT_UPDATED_SUCCESS(10501, HttpStatus.OK, "게시글 댓글이 성공적으로 수정되었습니다."),
    POST_COMMENT_DELETED_SUCCESS(10502, HttpStatus.OK, "게시글 댓글이 성공적으로 삭제되었습니다."),
}
