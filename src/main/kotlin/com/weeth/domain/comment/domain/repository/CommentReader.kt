package com.weeth.domain.comment.domain.repository

import com.weeth.domain.comment.domain.entity.Comment

interface CommentReader {
    fun findAllByPostId(postId: Long): List<Comment>
}
