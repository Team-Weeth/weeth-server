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
}
