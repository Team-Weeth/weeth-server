package com.weeth.domain.board.domain.repository

import java.time.LocalDateTime

interface NoticeReadReader {
    fun findReadPostIdsByUserId(
        userId: Long,
        since: LocalDateTime,
    ): Set<Long>
}
