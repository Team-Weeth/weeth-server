package com.weeth.domain.board.domain.repository

import com.weeth.domain.board.domain.entity.Board

interface BoardReader {
    fun findAllActiveByClubId(clubId: Long): List<Board>
}
