package com.weeth.domain.board.application.usecase.query

import com.weeth.domain.board.application.dto.response.BoardDetailResponse
import com.weeth.domain.board.application.dto.response.BoardListResponse
import com.weeth.domain.board.application.exception.BoardNotFoundException
import com.weeth.domain.board.application.mapper.BoardMapper
import com.weeth.domain.board.domain.repository.BoardRepository
import com.weeth.domain.user.domain.enums.Role
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetBoardQueryService(
    private val boardRepository: BoardRepository,
    private val boardMapper: BoardMapper,
) {
    // TODO(PR4): 해당 클럽 소속 멤버인지 검증 필요
    fun findBoards(
        clubId: Long,
        role: Role,
    ): List<BoardListResponse> =
        boardRepository
            .findAllByClubIdAndIsDeletedFalseOrderByIdAsc(clubId)
            .filter { it.isAccessibleBy(role) }
            .map(boardMapper::toListResponse)

    // TODO(PR4): 해당 클럽 소속 admin인지 검증 필요
    fun findBoardDetailForAdmin(
        clubId: Long,
        boardId: Long,
    ): BoardDetailResponse {
        val board = boardRepository.findByIdAndClubId(boardId, clubId) ?: throw BoardNotFoundException()
        return boardMapper.toDetailResponseForAdmin(board)
    }

    // TODO(PR4): 해당 클럽 소속 admin인지 검증 필요
    fun findAllBoardsForAdmin(clubId: Long): List<BoardDetailResponse> =
        boardRepository
            .findAllByClubIdOrderByIdAsc(clubId)
            .map(boardMapper::toDetailResponseForAdmin)
}
