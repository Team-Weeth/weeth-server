package com.weeth.domain.board.domain.repository

import com.weeth.domain.board.domain.entity.LastNoticeRead

interface LastNoticeReadReader {
    fun findByUserIdAndBoardId(
        userId: Long,
        boardId: Long,
    ): LastNoticeRead?
}
