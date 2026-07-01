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

    fun findByPostAndUserId(
        post: Post,
        userId: Long,
    ): PostLike?

    @Query(
        """
        SELECT p.id
        FROM PostLike pl
        JOIN pl.post p
        JOIN p.board b
        WHERE pl.userId = :userId
          AND b.club.id = :clubId
          AND pl.isActive = true
          AND pl.deletedAt IS NULL
        ORDER BY p.id ASC
        """,
    )
    fun findActivePostIdsByUserIdAndClubId(
        @Param("userId") userId: Long,
        @Param("clubId") clubId: Long,
    ): List<Long>

    @Query(
        """
        SELECT p.id
        FROM PostLike pl
        JOIN pl.post p
        JOIN p.board b
        WHERE pl.userId = :userId
          AND b.club.id IN :clubIds
          AND pl.isActive = true
          AND pl.deletedAt IS NULL
        ORDER BY p.id ASC
        """,
    )
    fun findActivePostIdsByUserIdAndClubIdIn(
        @Param("userId") userId: Long,
        @Param("clubIds") clubIds: List<Long>,
    ): List<Long>

    @Query(
        """
        SELECT pl
        FROM PostLike pl
        JOIN FETCH pl.post p
        WHERE pl.userId = :userId
          AND p.id IN :postIds
          AND pl.isActive = true
          AND pl.deletedAt IS NULL
        ORDER BY p.id ASC
        """,
    )
    fun findAllActiveByUserIdAndPostIds(
        @Param("userId") userId: Long,
        @Param("postIds") postIds: List<Long>,
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
