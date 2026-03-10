package com.weeth.domain.board.application.usecase.command

import com.weeth.domain.board.domain.entity.NoticeRead
import com.weeth.domain.board.domain.enums.BoardType
import com.weeth.domain.board.domain.repository.NoticeReadReader
import com.weeth.domain.board.domain.repository.NoticeReadRepository
import com.weeth.domain.board.domain.repository.PostReader
import com.weeth.domain.user.domain.repository.UserReader
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class MarkNoticeReadUseCase(
    private val postReader: PostReader,
    private val noticeReadReader: NoticeReadReader,
    private val noticeReadRepository: NoticeReadRepository,
    private val userReader: UserReader,
) {
    @Transactional
    fun execute(userId: Long) {
        val recentNotices = postReader.findRecentByBoardTypeSince(BoardType.NOTICE, LocalDateTime.now().minusWeeks(2))
        val readPostIds = noticeReadReader.findReadPostIdsByUserId(userId)
        val unreadNotices = recentNotices.filter { it.id !in readPostIds }

        if (unreadNotices.isEmpty()) return

        val user = userReader.getById(userId)

        noticeReadRepository.saveAll(
            unreadNotices.map { NoticeRead.create(user = user, post = it) },
        )
    }
}
