package com.weeth.domain.board.application.usecase.query

import com.weeth.domain.board.application.dto.response.BoardDetailResponse
import com.weeth.domain.board.application.dto.response.BoardListResponse
import com.weeth.domain.board.application.exception.BoardNotFoundException
import com.weeth.domain.board.application.mapper.BoardMapper
import com.weeth.domain.board.domain.repository.BoardRepository
import com.weeth.domain.user.domain.entity.enums.Role
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetBoardQueryService(
    private val boardRepository: BoardRepository,
    private val boardMapper: BoardMapper,
) {
    fun findBoards(role: Role): List<BoardListResponse> =
        boardRepository
            .findAllByIsDeletedFalseOrderByIdAsc()
            .filter { it.isAccessibleBy(role) }
            .map(boardMapper::toListResponse)

    fun findBoard(
        boardId: Long,
        role: Role,
    ): BoardDetailResponse {
        val board =
            boardRepository
                .findByIdAndIsDeletedFalse(boardId)
                ?.takeIf { it.isAccessibleBy(role) }
                ?: throw BoardNotFoundException()
        return boardMapper.toDetailResponse(board)
    }

    fun findAllBoardsForAdmin(): List<BoardDetailResponse> =
        boardRepository
            .findAllByIsDeletedFalseOrderByIdAsc()
            .map(boardMapper::toDetailResponseForAdmin)
}
