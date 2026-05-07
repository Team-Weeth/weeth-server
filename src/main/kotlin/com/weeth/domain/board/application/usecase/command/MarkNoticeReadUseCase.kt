package com.weeth.domain.board.application.usecase.command

import com.weeth.domain.board.application.exception.BoardNotFoundException
import com.weeth.domain.board.application.exception.BoardNotInClubException
import com.weeth.domain.board.application.exception.BoardTypeMismatchException
import com.weeth.domain.board.domain.entity.LastNoticeRead
import com.weeth.domain.board.domain.enums.BoardType
import com.weeth.domain.board.domain.repository.BoardRepository
import com.weeth.domain.board.domain.repository.LastNoticeReadReader
import com.weeth.domain.board.domain.repository.LastNoticeReadRepository
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class MarkNoticeReadUseCase(
    private val boardRepository: BoardRepository,
    private val lastNoticeReadReader: LastNoticeReadReader,
    private val lastNoticeReadRepository: LastNoticeReadRepository,
    private val clubMemberPolicy: ClubMemberPolicy,
) {
    @Transactional
    fun execute(
        userId: Long,
        clubId: Long,
        boardId: Long,
    ) {
        val clubMember = clubMemberPolicy.getActiveMember(clubId, userId)

        val board =
            boardRepository.findByIdAndIsDeletedFalse(boardId)
                ?: throw BoardNotFoundException()
        if (board.club.id != clubId) throw BoardNotInClubException()
        if (board.type != BoardType.NOTICE) throw BoardTypeMismatchException()

        val existing = lastNoticeReadReader.findByClubMemberIdAndBoardId(clubMember.id, boardId)
        if (existing != null) {
            existing.updateLastReadAt(LocalDateTime.now())
            return
        }

        lastNoticeReadRepository.save(LastNoticeRead.create(clubMember = clubMember, board = board))
    }
}
