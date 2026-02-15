package com.weeth.domain.comment.fixture

import com.weeth.domain.board.domain.entity.Notice
import com.weeth.domain.comment.domain.entity.Comment
import com.weeth.domain.user.domain.entity.User

object CommentTestFixture {
    fun createComment(
        id: Long,
        content: String,
        user: User,
        notice: Notice,
    ): Comment =
        Comment
            .builder()
            .id(id)
            .content(content)
            .notice(notice)
            .user(user)
            .children(ArrayList())
            .isDeleted(false)
            .build()
}
