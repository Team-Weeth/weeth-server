package com.weeth.domain.comment.fixture

import com.weeth.domain.board.domain.entity.Post
import com.weeth.domain.comment.domain.entity.Comment
import com.weeth.domain.user.domain.entity.User

object CommentTestFixture {
    fun createPostComment(
        id: Long = 1L,
        content: String = "테스트 댓글",
        post: Post,
        user: User,
        parent: Comment? = null,
        isDeleted: Boolean = false,
    ) = Comment(
        id = id,
        content = content,
        post = post,
        user = user,
        parent = parent,
        isDeleted = isDeleted,
    )
}
