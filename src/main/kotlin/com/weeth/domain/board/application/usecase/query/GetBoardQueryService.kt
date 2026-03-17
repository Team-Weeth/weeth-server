package com.weeth.domain.board.application.usecase.query

import com.weeth.domain.board.application.dto.response.BoardDetailResponse
import com.weeth.domain.board.application.dto.response.BoardListResponse
import com.weeth.domain.board.application.exception.BoardNotFoundException
import com.weeth.domain.board.application.mapper.BoardMapper
import com.weeth.domain.board.domain.repository.BoardRepository
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetBoardQueryService(
    private val boardRepository: BoardRepository,
    private val clubMemberPolicy: ClubMemberPolicy,
    private val boardMapper: BoardMapper,
) {
    fun findBoards(
        clubId: Long,
        userId: Long,
    ): List<BoardListResponse> {
        val member = clubMemberPolicy.getActiveMember(clubId, userId)

        return boardRepository
            .findAllByClubIdAndIsDeletedFalseOrderByIdAsc(clubId)
            .filter { it.isAccessibleBy(member.memberRole) }
            .map(boardMapper::toListResponse)
    }

    fun findBoardDetailForAdmin(
        clubId: Long,
        userId: Long,
        boardId: Long,
    ): BoardDetailResponse {
        clubMemberPolicy.requireAdmin(clubId, userId)
        val board = boardRepository.findByIdAndClubId(boardId, clubId) ?: throw BoardNotFoundException()

        return boardMapper.toDetailResponseForAdmin(board)
    }

    fun findAllBoardsForAdmin(
        clubId: Long,
        userId: Long,
    ): List<BoardDetailResponse> {
        clubMemberPolicy.requireAdmin(clubId, userId)

        return boardRepository
            .findAllByClubIdOrderByIdAsc(clubId)
            .map(boardMapper::toDetailResponseForAdmin)
    }
}
