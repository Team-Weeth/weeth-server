package com.weeth.domain.board.domain.repository

import com.weeth.domain.board.domain.entity.NoticeRead
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface NoticeReadRepository :
    JpaRepository<NoticeRead, Long>,
    NoticeReadReader {
    @Query("SELECT nr.post.id FROM NoticeRead nr WHERE nr.user.id = :userId AND nr.post.createdAt >= :since")
    override fun findReadPostIdsByUserId(
        @Param("userId") userId: Long,
        @Param("since") since: LocalDateTime,
    ): Set<Long>
}
