package com.weeth.domain.board.application.usecase.command

import com.weeth.domain.board.application.exception.BoardNotFoundException
import com.weeth.domain.board.application.exception.BoardNotInClubException
import com.weeth.domain.board.application.exception.BoardTypeMismatchException
import com.weeth.domain.board.domain.enums.BoardType
import com.weeth.domain.board.domain.repository.BoardRepository
import com.weeth.domain.board.domain.repository.LastNoticeReadRepository
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class MarkNoticeReadUseCase(
    private val boardRepository: BoardRepository,
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

        lastNoticeReadRepository.markRead(
            clubMemberId = clubMember.id,
            boardId = board.id,
            lastReadAt = LocalDateTime.now(),
        )
    }
}
