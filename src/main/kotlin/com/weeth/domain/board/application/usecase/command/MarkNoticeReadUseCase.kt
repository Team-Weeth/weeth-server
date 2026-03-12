package com.weeth.domain.board.application.usecase.command

import com.weeth.domain.board.application.exception.BoardNotFoundException
import com.weeth.domain.board.application.exception.BoardTypeMismatchException
import com.weeth.domain.board.domain.entity.LastNoticeRead
import com.weeth.domain.board.domain.enums.BoardType
import com.weeth.domain.board.domain.repository.BoardRepository
import com.weeth.domain.board.domain.repository.LastNoticeReadReader
import com.weeth.domain.board.domain.repository.LastNoticeReadRepository
import com.weeth.domain.user.domain.repository.UserReader
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class MarkNoticeReadUseCase(
    private val boardRepository: BoardRepository,
    private val lastNoticeReadReader: LastNoticeReadReader,
    private val lastNoticeReadRepository: LastNoticeReadRepository,
    private val userReader: UserReader,
) {
    @Transactional
    fun execute(
        userId: Long,
        boardId: Long,
    ) {
        val board =
            boardRepository.findByIdAndIsDeletedFalse(boardId)
                ?: throw BoardNotFoundException()
        if (board.type != BoardType.NOTICE) throw BoardTypeMismatchException()
        // TODO: 해당 클럽 회원인지 검증 후 클럽의 공지만 읽음 처리

        val existing = lastNoticeReadReader.findByUserIdAndBoardId(userId, boardId)
        if (existing != null) {
            existing.updateLastReadAt(LocalDateTime.now())
            return
        }

        val user = userReader.getById(userId)
        lastNoticeReadRepository.save(LastNoticeRead.create(user = user, board = board))
    }
}
