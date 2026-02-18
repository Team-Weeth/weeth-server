package com.weeth.domain.comment.domain.entity

import com.fasterxml.jackson.annotation.JsonBackReference
import com.weeth.domain.board.domain.entity.Notice
import com.weeth.domain.board.domain.entity.Post
import com.weeth.domain.comment.domain.vo.CommentContent
import com.weeth.domain.user.domain.entity.User
import com.weeth.global.common.entity.BaseEntity
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity
@Table(name = "comment")
class Comment(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    val id: Long = 0,
    @Column(length = 300, nullable = false)
    var content: String,
    @Column(nullable = false)
    var isDeleted: Boolean = false,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    val post: Post? = null, // Todo: Board 도메인 리팩토링시 반영
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notice_id")
    val notice: Notice? = null, // Todo: Board 도메인 리팩토링시 반영
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    val user: User,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    val parent: Comment? = null,
    @OneToMany(mappedBy = "parent", cascade = [CascadeType.REMOVE], fetch = FetchType.LAZY)
    val children: MutableList<Comment> = mutableListOf(),
) : BaseEntity() {
    fun markAsDeleted() {
        isDeleted = true
        content = DELETED_CONTENT
    }

    fun getIsDeleted(): Boolean = isDeleted

    fun updateContent(newContent: String) {
        content = CommentContent.from(newContent).value
    }

    fun isOwnedBy(userId: Long): Boolean = user.id == userId

    companion object {
        private const val DELETED_CONTENT = "삭제된 댓글입니다."

        fun createForPost(
            content: String,
            post: Post,
            user: User,
            parent: Comment?,
        ): Comment {
            require(parent == null || parent.post?.id == post.id) {
                "부모 댓글은 동일한 게시글에 존재해야 합니다."
            }
            return Comment(
                content = CommentContent.from(content).value,
                post = post,
                user = user,
                parent = parent,
            )
        }

        fun createForNotice(
            content: String,
            notice: Notice,
            user: User,
            parent: Comment?,
        ): Comment {
            require(parent == null || parent.notice?.id == notice.id) {
                "부모 댓글은 동일한 공지글에 존재해야 합니다."
            }
            return Comment(
                content = CommentContent.from(content).value,
                notice = notice,
                user = user,
                parent = parent,
            )
        }
    }
}
