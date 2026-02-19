package com.weeth.domain.board.fixture

import com.weeth.domain.board.domain.entity.Post
import com.weeth.domain.user.domain.entity.User
import com.weeth.domain.user.fixture.UserTestFixture

object PostTestFixture {
    fun create(
        id: Long = 2L,
        title: String = "게시글",
        content: String = "내용",
        user: User = UserTestFixture.createActiveUser1(1L),
    ): Post =
        Post(
            id = id,
            title = title,
            content = content,
            user = user,
            board = BoardTestFixture.create(),
        )
}
