package com.weeth.domain.comment.domain.repository

import com.weeth.domain.comment.domain.entity.Comment
import jakarta.persistence.LockModeType
import jakarta.persistence.QueryHint
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import org.springframework.data.repository.query.Param

interface CommentRepository :
    JpaRepository<Comment, Long>,
    CommentReader {
    @EntityGraph(attributePaths = ["clubMember", "clubMember.user", "clubMember.userProfile"])
    fun findByIdAndPostId(
        id: Long,
        postId: Long,
    ): Comment?

    @EntityGraph(attributePaths = ["clubMember", "clubMember.user", "clubMember.userProfile"])
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(QueryHint(name = "jakarta.persistence.lock.timeout", value = "2000"))
    @Query(
        """
        SELECT c
        FROM Comment c
        WHERE c.id = :id
          AND c.post.id = :postId
        """,
    )
    fun findByIdAndPostIdWithLock(
        @Param("id") id: Long,
        @Param("postId") postId: Long,
    ): Comment?

    @EntityGraph(attributePaths = ["clubMember", "clubMember.user", "clubMember.userProfile"])
    @Query("SELECT c FROM Comment c WHERE c.post.id = :postId")
    override fun findAllByPostId(
        @Param("postId") postId: Long,
    ): List<Comment>

    @Query(
        """
        SELECT c.id
        FROM Comment c
        WHERE c.clubMember.id IN :clubMemberIds
          AND c.isDeleted = false
          AND c.post.isDeleted = false
          AND c.post.board.isDeleted = false
        ORDER BY c.id ASC
        """,
    )
    fun findActiveIdsByClubMemberIdIn(
        @Param("clubMemberIds") clubMemberIds: List<Long>,
    ): List<Long>
}
