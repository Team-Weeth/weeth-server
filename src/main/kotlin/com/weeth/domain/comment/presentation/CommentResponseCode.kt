package com.weeth.domain.comment.presentation

import com.weeth.global.common.response.ResponseCodeInterface
import org.springframework.http.HttpStatus

enum class CommentResponseCode(
    override val code: Int,
    override val status: HttpStatus,
    override val message: String,
) : ResponseCodeInterface {
    COMMENT_CREATED_SUCCESS(1400, HttpStatus.OK, "공지사항 댓글이 성공적으로 생성되었습니다."),
    COMMENT_UPDATED_SUCCESS(1401, HttpStatus.OK, "공지사항 댓글이 성공적으로 수정되었습니다."),
    COMMENT_DELETED_SUCCESS(1402, HttpStatus.OK, "공지사항 댓글이 성공적으로 삭제되었습니다."),
    POST_COMMENT_CREATED_SUCCESS(1403, HttpStatus.OK, "게시글 댓글이 성공적으로 생성되었습니다."),
    POST_COMMENT_UPDATED_SUCCESS(1404, HttpStatus.OK, "게시글 댓글이 성공적으로 수정되었습니다."),
    POST_COMMENT_DELETED_SUCCESS(1405, HttpStatus.OK, "게시글 댓글이 성공적으로 삭제되었습니다."),
}
