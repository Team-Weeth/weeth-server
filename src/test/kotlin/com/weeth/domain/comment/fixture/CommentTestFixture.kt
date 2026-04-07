package com.weeth.domain.comment.fixture

import com.weeth.domain.board.domain.entity.Post
import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.club.fixture.ClubMemberTestFixture
import com.weeth.domain.comment.domain.entity.Comment

object CommentTestFixture {
    fun createPostComment(
        id: Long = 1L,
        content: String = "테스트 댓글",
        post: Post,
        clubMember: ClubMember = ClubMemberTestFixture.createActiveMember(),
        parent: Comment? = null,
        isDeleted: Boolean = false,
    ) = Comment(
        id = id,
        content = content,
        post = post,
        clubMember = clubMember,
        parent = parent,
        isDeleted = isDeleted,
    )
}
