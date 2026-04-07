package com.weeth.domain.board.fixture

import com.weeth.domain.board.domain.entity.Board
import com.weeth.domain.board.domain.entity.Post
import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.club.fixture.ClubMemberTestFixture

object PostTestFixture {
    fun create(
        title: String = "게시글",
        content: String = "내용",
        clubMember: ClubMember = ClubMemberTestFixture.createActiveMember(),
        board: Board = BoardTestFixture.create(),
        cardinalNumber: Int? = null,
        initialLikeCount: Int = 0,
    ): Post =
        Post(
            title = title,
            content = content,
            clubMember = clubMember,
            board = board,
            cardinalNumber = cardinalNumber,
        ).also { post ->
            repeat(initialLikeCount) { post.increaseLikeCount() }
        }
}
