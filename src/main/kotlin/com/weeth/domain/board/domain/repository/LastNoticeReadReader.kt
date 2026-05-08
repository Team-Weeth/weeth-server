package com.weeth.domain.board.domain.repository

import com.weeth.domain.board.domain.entity.LastNoticeRead

interface LastNoticeReadReader {
    fun findByClubMemberIdAndBoardId(
        clubMemberId: Long,
        boardId: Long,
    ): LastNoticeRead?
}
