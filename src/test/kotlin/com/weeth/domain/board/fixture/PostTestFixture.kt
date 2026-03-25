package com.weeth.domain.board.fixture

import com.weeth.domain.board.domain.entity.Board
import com.weeth.domain.board.domain.entity.Post
import com.weeth.domain.user.domain.entity.User
import com.weeth.domain.user.fixture.UserTestFixture

object PostTestFixture {
    fun create(
        title: String = "게시글",
        content: String = "내용",
        user: User = UserTestFixture.createActiveUser1(1L),
        board: Board = BoardTestFixture.create(),
        cardinalNumber: Int? = null,
        initialLikeCount: Int = 0,
    ): Post =
        Post(
            title = title,
            content = content,
            user = user,
            board = board,
            cardinalNumber = cardinalNumber,
        ).also { post ->
            repeat(initialLikeCount) { post.increaseLikeCount() }
        }
}
