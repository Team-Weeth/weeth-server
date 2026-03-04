package com.weeth.domain.board.domain.entity

import com.weeth.domain.user.domain.entity.User
import com.weeth.global.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "post")
class Post(
    title: String,
    content: String,
    user: User,
    board: Board,
    cardinalNumber: Int? = null,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L
        private set

    @Column(nullable = false)
    var title: String = title
        private set

    @Column(columnDefinition = "TEXT", nullable = false)
    var content: String = content
        private set

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User = user
        private set

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "board_id", nullable = false)
    var board: Board = board
        private set

    @Column(nullable = false)
    var commentCount: Int = 0
        private set

    @Column(nullable = false)
    var likeCount: Int = 0
        private set

    @Column
    var cardinalNumber: Int? = cardinalNumber
        private set

    @Column(nullable = false)
    var isDeleted: Boolean = false
        private set

    fun increaseCommentCount() {
        commentCount++
    }

    fun decreaseCommentCount() {
        check(commentCount > 0) { "댓글 수는 0보다 작아질 수 없습니다" }
        commentCount--
    }

    fun increaseLikeCount() {
        likeCount++
    }

    fun decreaseLikeCount() {
        check(likeCount > 0) { "좋아요 수는 0보다 작아질 수 없습니다" }
        likeCount--
    }

    fun isOwnedBy(userId: Long): Boolean = user.id == userId

    fun update(
        newTitle: String?,
        newContent: String?,
        newCardinalNumber: Int?,
    ) {
        newTitle?.let {
            require(it.isNotBlank()) { "제목은 비어 있을 수 없습니다" }
            title = it
        }
        newContent?.let {
            require(it.isNotBlank()) { "내용은 비어 있을 수 없습니다" }
            content = it
        }
        newCardinalNumber?.let { cardinalNumber = it }
    }

    fun markDeleted() {
        isDeleted = true
    }

    fun restore() {
        isDeleted = false
    }

    companion object {
        fun create(
            title: String,
            content: String,
            user: User,
            board: Board,
            cardinalNumber: Int? = null,
        ): Post {
            require(title.isNotBlank()) { "제목은 비어 있을 수 없습니다" }
            require(content.isNotBlank()) { "내용은 비어 있을 수 없습니다" }
            return Post(
                title = title,
                content = content,
                user = user,
                board = board,
                cardinalNumber = cardinalNumber,
            )
        }
    }
}
