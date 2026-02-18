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
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(nullable = false)
    var title: String,
    @Column(columnDefinition = "TEXT", nullable = false)
    var content: String,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    val user: User,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id")
    val board: Board,
    @Column(nullable = false)
    var commentCount: Int = 0,
    @Column(nullable = false)
    var likeCount: Int = 0,
    @Column
    var cardinalNumber: Int? = null,
) : BaseEntity() {
    fun increaseCommentCount() {
        commentCount++
    }

    fun decreaseCommentCount() {
        check(commentCount > 0) { "comment count cannot be negative" }
        commentCount--
    }

    fun increaseLikeCount() {
        likeCount++
    }

    fun decreaseLikeCount() {
        check(likeCount > 0) { "like count cannot be negative" }
        likeCount--
    }

    fun updateContent(
        newTitle: String,
        newContent: String,
    ) {
        require(newTitle.isNotBlank()) { "title must not be blank" }
        title = newTitle
        content = newContent
    }

    fun isOwnedBy(userId: Long): Boolean = user.id == userId

    fun update(
        newTitle: String,
        newContent: String,
        newCardinalNumber: Int?,
    ) {
        updateContent(newTitle, newContent)
        cardinalNumber = newCardinalNumber
    }

    companion object {
        fun create(
            title: String,
            content: String,
            user: User,
            board: Board,
            cardinalNumber: Int? = null,
        ): Post {
            require(title.isNotBlank()) { "title must not be blank" }
            require(content.isNotBlank()) { "content must not be blank" }
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
