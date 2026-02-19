package com.weeth.domain.schedule.domain.entity

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
import java.time.LocalDateTime

@Entity
class Event(
    var title: String,

    @Column(columnDefinition = "TEXT")
    var content: String,

    var location: String,

    var cardinal: Int,

    var requiredItem: String? = null,

    var start: LocalDateTime,

    var end: LocalDateTime,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    var user: User? = null,
) : BaseEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    fun update(
        title: String,
        content: String,
        location: String,
        requiredItem: String?,
        start: LocalDateTime,
        end: LocalDateTime,
        user: User?,
    ) {
        this.title = title
        this.content = content
        this.location = location
        this.requiredItem = requiredItem
        this.start = start
        this.end = end
        this.user = user
    }

    companion object {
        fun create(
            title: String,
            content: String,
            location: String,
            cardinal: Int,
            requiredItem: String?,
            start: LocalDateTime,
            end: LocalDateTime,
            user: User?,
        ): Event {
            require(title.isNotBlank()) { "제목은 필수입니다" }
            require(!end.isBefore(start)) { "종료 시간은 시작 시간 이후여야 합니다" }
            return Event(
                title = title,
                content = content,
                location = location,
                cardinal = cardinal,
                requiredItem = requiredItem,
                start = start,
                end = end,
                user = user,
            )
        }
    }
}
