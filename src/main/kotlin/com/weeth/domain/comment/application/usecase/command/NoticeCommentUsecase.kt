package com.weeth.domain.comment.application.usecase.command

import com.weeth.domain.comment.application.dto.request.CommentSaveRequest
import com.weeth.domain.comment.application.dto.request.CommentUpdateRequest

interface NoticeCommentUsecase {
    fun saveNoticeComment(
        dto: CommentSaveRequest,
        noticeId: Long,
        userId: Long,
    )

    fun updateNoticeComment(
        dto: CommentUpdateRequest,
        noticeId: Long,
        commentId: Long,
        userId: Long,
    )

    fun deleteNoticeComment(
        noticeId: Long,
        commentId: Long,
        userId: Long,
    )
}
