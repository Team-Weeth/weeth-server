package com.weeth.domain.board.domain.entity

import com.weeth.domain.user.domain.entity.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

@Entity
@Table(
    name = "last_notice_read",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "board_id"])],
)
class LastNoticeRead(
    user: User,
    board: Board,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L
        private set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User = user
        private set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    var board: Board = board
        private set

    @Column(nullable = false)
    var lastReadAt: LocalDateTime = LocalDateTime.now()
        private set

    fun updateLastReadAt(time: LocalDateTime) {
        lastReadAt = time
    }

    companion object {
        fun create(
            user: User,
            board: Board,
        ) = LastNoticeRead(user = user, board = board)
    }
}
