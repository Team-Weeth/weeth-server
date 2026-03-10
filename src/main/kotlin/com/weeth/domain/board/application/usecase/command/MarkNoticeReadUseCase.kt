package com.weeth.domain.board.application.usecase.command

import com.weeth.domain.board.application.exception.BoardNotFoundException
import com.weeth.domain.board.application.exception.BoardTypeMismatchException
import com.weeth.domain.board.domain.entity.NoticeRead
import com.weeth.domain.board.domain.enums.BoardType
import com.weeth.domain.board.domain.repository.BoardRepository
import com.weeth.domain.board.domain.repository.NoticeReadReader
import com.weeth.domain.board.domain.repository.NoticeReadRepository
import com.weeth.domain.board.domain.repository.PostReader
import com.weeth.domain.user.domain.repository.UserReader
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class MarkNoticeReadUseCase(
    private val boardRepository: BoardRepository,
    private val postReader: PostReader,
    private val noticeReadReader: NoticeReadReader,
    private val noticeReadRepository: NoticeReadRepository,
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

        val since = LocalDateTime.now().minusWeeks(2)
        val recentNotices = postReader.findRecentByBoardIdSince(boardId, since)
        val readPostIds = noticeReadReader.findReadPostIdsByUserId(userId, since)
        val unreadNotices = recentNotices.filter { it.id !in readPostIds }

        if (unreadNotices.isEmpty()) return

        val user = userReader.getById(userId)

        try {
            noticeReadRepository.saveAll(
                unreadNotices.map { NoticeRead.create(user = user, post = it) },
            )
        } catch (e: DataIntegrityViolationException) {
            // 동시 요청으로 이미 저장된 경우 무시
        }
    }
}
