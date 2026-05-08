package com.weeth.domain.board.domain.repository

import com.weeth.domain.board.domain.entity.LastNoticeRead
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface LastNoticeReadRepository :
    JpaRepository<LastNoticeRead, Long>,
    LastNoticeReadReader {
    override fun findByClubMemberIdAndBoardId(
        clubMemberId: Long,
        boardId: Long,
    ): LastNoticeRead?

    @Modifying
    @Query(
        value = """
            INSERT INTO last_notice_read (club_member_id, board_id, last_read_at)
            VALUES (:clubMemberId, :boardId, :lastReadAt)
            ON DUPLICATE KEY UPDATE last_read_at = :lastReadAt
        """,
        nativeQuery = true,
    )
    fun markRead(
        @Param("clubMemberId") clubMemberId: Long,
        @Param("boardId") boardId: Long,
        @Param("lastReadAt") lastReadAt: LocalDateTime,
    ): Int
}
