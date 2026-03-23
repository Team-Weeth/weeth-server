package com.weeth.domain.board.domain.repository

import com.weeth.domain.board.domain.entity.Board
import org.springframework.data.jpa.repository.JpaRepository

interface BoardRepository : JpaRepository<Board, Long> {
    fun findByIdAndIsDeletedFalse(id: Long): Board?

    fun findByIdAndClubId(
        boardId: Long,
        clubId: Long,
    ): Board?

    fun findAllByClubIdAndIsDeletedFalseOrderByDisplayOrderAscIdAsc(clubId: Long): List<Board>

    fun findByIdAndClubIdAndIsDeletedFalse(
        boardId: Long,
        clubId: Long,
    ): Board?

    fun findAllByClubIdOrderByDisplayOrderAscIdAsc(clubId: Long): List<Board>

    fun findAllByClubIdAndIdIn(
        clubId: Long,
        ids: List<Long>,
    ): List<Board>
}
