package com.weeth.domain.comment.application.usecase.command

import com.weeth.domain.comment.application.dto.request.CommentSaveRequest
import com.weeth.domain.comment.application.dto.request.CommentUpdateRequest

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
