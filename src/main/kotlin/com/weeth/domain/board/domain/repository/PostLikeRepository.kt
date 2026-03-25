package com.weeth.domain.board.domain.repository

import com.weeth.domain.board.domain.entity.Post
import com.weeth.domain.board.domain.entity.PostLike
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface PostLikeRepository : JpaRepository<PostLike, Long> {
    fun existsByPostAndUserIdAndIsActiveTrue(
        post: Post,
        userId: Long,
    ): Boolean

    fun findByPostAndUserId(
        post: Post,
        userId: Long,
    ): PostLike?

    @Query(
        """
        SELECT pl.post.id
        FROM PostLike pl
        WHERE pl.post.id IN :postIds
          AND pl.userId = :userId
          AND pl.isActive = true
        """,
    )
    fun findLikedPostIds(
        postIds: List<Long>,
        userId: Long,
    ): Set<Long>
}
