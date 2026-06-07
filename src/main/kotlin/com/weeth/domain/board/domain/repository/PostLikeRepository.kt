package com.weeth.domain.board.domain.repository

import com.weeth.domain.board.domain.entity.Post
import com.weeth.domain.board.domain.entity.PostLike
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface PostLikeRepository :
    JpaRepository<PostLike, Long>,
    PostLikeReader {
    fun existsByPostAndUserIdAndIsActiveTrueAndDeletedAtIsNull(
        post: Post,
        userId: Long,
    ): Boolean

    fun findByPostAndUserIdAndDeletedAtIsNull(
        post: Post,
        userId: Long,
    ): PostLike?

    @Query(
        """
        SELECT pl
        FROM PostLike pl
        JOIN FETCH pl.post p
        JOIN p.board b
        WHERE pl.userId = :userId
          AND b.club.id = :clubId
          AND pl.isActive = true
          AND pl.deletedAt IS NULL
        ORDER BY p.id ASC
        """,
    )
    fun findAllActiveByUserIdAndClubId(
        @Param("userId") userId: Long,
        @Param("clubId") clubId: Long,
    ): List<PostLike>

    @Query(
        """
        SELECT pl.post.id
        FROM PostLike pl
        WHERE pl.post.id IN :postIds
          AND pl.userId = :userId
          AND pl.isActive = true
          AND pl.deletedAt IS NULL
        """,
    )
    override fun findLikedPostIds(
        postIds: List<Long>,
        userId: Long,
    ): Set<Long>
}
