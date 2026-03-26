package com.weeth.domain.board.domain.repository

import com.weeth.domain.board.domain.entity.Board
import com.weeth.domain.board.domain.enums.BoardType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface BoardRepository :
    JpaRepository<Board, Long>,
    BoardReader {
    fun findByIdAndIsDeletedFalse(id: Long): Board?

    fun findByIdAndClubId(
        boardId: Long,
        clubId: Long,
    ): Board?

    fun findAllByClubIdAndIsDeletedFalseOrderByDisplayOrderAscIdAsc(clubId: Long): List<Board>

    override fun findAllActiveByClubId(clubId: Long): List<Board> =
        findAllByClubIdAndIsDeletedFalseOrderByDisplayOrderAscIdAsc(clubId)

    fun findByIdAndClubIdAndIsDeletedFalse(
        boardId: Long,
        clubId: Long,
    ): Board?

    fun findAllByClubIdOrderByDisplayOrderAscIdAsc(clubId: Long): List<Board>

    @Query("SELECT COALESCE(MAX(b.displayOrder), -1) FROM Board b WHERE b.club.id = :clubId AND b.isDeleted = false")
    fun findMaxActiveDisplayOrderByClubId(clubId: Long): Int

    @Query("SELECT COALESCE(MAX(b.displayOrder), -1) FROM Board b WHERE b.club.id = :clubId")
    fun findMaxDisplayOrderByClubId(clubId: Long): Int

    fun countByClubIdAndTypeNotAndIsDeletedFalse(
        clubId: Long,
        type: BoardType,
    ): Int

    fun existsByClubIdAndNameAndIsDeletedFalse(
        clubId: Long,
        name: String,
    ): Boolean

    fun existsByClubIdAndNameAndIsDeletedFalseAndIdNot(
        clubId: Long,
        name: String,
        id: Long,
    ): Boolean
}
