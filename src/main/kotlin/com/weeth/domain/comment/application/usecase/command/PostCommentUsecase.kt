package com.weeth.domain.comment.application.usecase.command

import com.weeth.domain.comment.application.dto.request.CommentSaveRequest
import com.weeth.domain.comment.application.dto.request.CommentUpdateRequest

/**
 * Todo: Notice가 제거됨에 따라 인터페이스 분리가 필요 없음. 제거 검토
 */
interface PostCommentUsecase {
    fun savePostComment(
        dto: CommentSaveRequest,
        postId: Long,
        userId: Long,
    )

    fun updatePostComment(
        dto: CommentUpdateRequest,
        postId: Long,
        commentId: Long,
        userId: Long,
    )

    fun deletePostComment(
        postId: Long,
        commentId: Long,
        userId: Long,
    )
}
