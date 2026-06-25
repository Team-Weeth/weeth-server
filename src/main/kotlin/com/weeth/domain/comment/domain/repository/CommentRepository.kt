package com.weeth.domain.comment.domain.repository

import com.weeth.domain.comment.domain.entity.Comment
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface CommentRepository :
    JpaRepository<Comment, Long>,
    CommentReader {
    @EntityGraph(attributePaths = ["clubMember", "clubMember.user"])
    fun findByIdAndPostId(
        id: Long,
        postId: Long,
    ): Comment?

    @EntityGraph(attributePaths = ["clubMember", "clubMember.user"])
    @Query("SELECT c FROM Comment c WHERE c.post.id = :postId")
    override fun findAllByPostId(
        @Param("postId") postId: Long,
    ): List<Comment>

    @Query(
        """
        SELECT c.id
        FROM Comment c
        WHERE c.clubMember.id = :clubMemberId
          AND c.post.board.club.id = :clubId
          AND c.isDeleted = false
          AND c.post.isDeleted = false
          AND c.post.board.isDeleted = false
        ORDER BY c.id ASC
        """,
    )
    fun findActiveIdsByClubMemberIdAndClubId(
        @Param("clubMemberId") clubMemberId: Long,
        @Param("clubId") clubId: Long,
    ): List<Long>
}
