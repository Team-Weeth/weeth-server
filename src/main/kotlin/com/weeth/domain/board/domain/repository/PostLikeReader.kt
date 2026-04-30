package com.weeth.domain.board.domain.repository

interface PostLikeReader {
    fun findLikedPostIds(
        postIds: List<Long>,
        userId: Long,
    ): Set<Long>
}
