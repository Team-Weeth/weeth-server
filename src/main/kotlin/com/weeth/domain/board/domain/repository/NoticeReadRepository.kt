package com.weeth.domain.board.domain.repository

import com.weeth.domain.board.domain.entity.NoticeRead
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface NoticeReadRepository :
    JpaRepository<NoticeRead, Long>,
    NoticeReadReader {
    override fun existsByUserIdAndPostId(
        userId: Long,
        postId: Long,
    ): Boolean

    @Query("SELECT nr.post.id FROM NoticeRead nr WHERE nr.user.id = :userId")
    override fun findReadPostIdsByUserId(
        @Param("userId") userId: Long,
    ): Set<Long>
}
