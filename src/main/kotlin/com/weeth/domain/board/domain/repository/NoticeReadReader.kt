package com.weeth.domain.board.domain.repository

interface NoticeReadReader {
    fun findReadPostIdsByUserId(userId: Long): Set<Long>

    fun existsByUserIdAndPostId(
        userId: Long,
        postId: Long,
    ): Boolean
}
