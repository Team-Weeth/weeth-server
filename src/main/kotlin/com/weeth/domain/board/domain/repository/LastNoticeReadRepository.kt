package com.weeth.domain.board.domain.repository

import com.weeth.domain.board.domain.entity.LastNoticeRead
import org.springframework.data.jpa.repository.JpaRepository

interface LastNoticeReadRepository :
    JpaRepository<LastNoticeRead, Long>,
    LastNoticeReadReader {
    override fun findByUserIdAndBoardId(
        userId: Long,
        boardId: Long,
    ): LastNoticeRead?
}
