package com.weeth.domain.board.application.usecase.query

import com.weeth.domain.board.application.dto.response.BoardDetailResponse
import com.weeth.domain.board.application.dto.response.BoardListResponse
import com.weeth.domain.board.application.exception.BoardNotFoundException
import com.weeth.domain.board.application.mapper.BoardMapper
import com.weeth.domain.board.domain.repository.BoardRepository
import com.weeth.domain.user.domain.enums.Role
import org.springframework.data.repository.findByIdOrNull
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
            .filter { it.isAccessibleBy(role) } // todo: Club 기반 쿼리로 개선 시 DB 레벨 필터링으로 전환
            .map(boardMapper::toListResponse)

    fun findBoardDetailForAdmin(boardId: Long): BoardDetailResponse {
        val board = boardRepository.findByIdOrNull(boardId) ?: throw BoardNotFoundException()
        return boardMapper.toDetailResponseForAdmin(board)
    }

    fun findAllBoardsForAdmin(): List<BoardDetailResponse> =
        boardRepository
            .findAllByOrderByIdAsc()
            .map(boardMapper::toDetailResponseForAdmin)
}
