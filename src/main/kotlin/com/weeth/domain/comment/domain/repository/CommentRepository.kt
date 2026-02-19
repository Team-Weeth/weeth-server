package com.weeth.domain.comment.domain.repository

import com.weeth.domain.comment.domain.entity.Comment
import org.springframework.data.jpa.repository.JpaRepository

interface CommentRepository :
    JpaRepository<Comment, Long>,
    CommentReader {
    fun findByIdAndPostId(
        id: Long,
        postId: Long,
    ): Comment?
}
