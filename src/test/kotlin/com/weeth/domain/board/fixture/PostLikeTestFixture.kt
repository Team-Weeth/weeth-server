package com.weeth.domain.board.fixture

import com.weeth.domain.board.domain.entity.Post
import com.weeth.domain.board.domain.entity.PostLike

object PostLikeTestFixture {
    fun createActive(
        post: Post = PostTestFixture.create(),
        userId: Long = 1L,
    ): PostLike = PostLike(post = post, userId = userId)

    fun createInactive(
        post: Post = PostTestFixture.create(),
        userId: Long = 1L,
    ): PostLike = PostLike(post = post, userId = userId).also { it.toggle() }
}
