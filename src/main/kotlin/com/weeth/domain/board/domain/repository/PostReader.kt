package com.weeth.domain.board.domain.repository

import com.weeth.domain.board.domain.entity.Post
import com.weeth.domain.board.domain.enums.BoardType
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import java.time.LocalDateTime

interface PostReader {
    fun getById(postId: Long): Post

    fun findActiveById(postId: Long): Post?

    fun findRecentByBoardType(
        boardType: BoardType,
        pageable: Pageable,
    ): Slice<Post>

    fun findRecentExcludingBoardType(
        excludedType: BoardType,
        pageable: Pageable,
    ): Slice<Post>

    fun findRecentByClubIdAndBoardType(
        clubId: Long,
        boardType: BoardType,
        pageable: Pageable,
    ): Slice<Post>

    fun findRecentByClubIdExcludingBoardType(
        clubId: Long,
        excludedType: BoardType,
        pageable: Pageable,
    ): Slice<Post>

    fun findRecentByBoardIds(
        boardIds: List<Long>,
        pageable: Pageable,
    ): Slice<Post>

    fun findFirstUnreadNoticeSince(
        clubId: Long,
        userId: Long,
        boardType: BoardType,
        since: LocalDateTime,
    ): Post?
}
