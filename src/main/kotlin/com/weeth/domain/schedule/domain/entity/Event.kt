package com.weeth.domain.schedule.domain.entity

import com.weeth.domain.club.domain.entity.Club
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
    club: Club,
    var title: String,
    @Column(length = 500)
    var content: String? = null,
    var location: String? = null,
    var cardinal: Int,
    var start: LocalDateTime,
    var end: LocalDateTime,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    var user: User? = null,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    var club: Club = club
        private set

    fun update(
        title: String,
        content: String?,
        location: String?,
        start: LocalDateTime,
        end: LocalDateTime,
        user: User?,
    ) {
        require(title.isNotBlank()) { "제목은 필수입니다" }
        require(!end.isBefore(start)) { "종료 시간은 시작 시간 이후여야 합니다" }
        this.title = title
        this.content = content
        this.location = location
        this.start = start
        this.end = end
        this.user = user
    }

    companion object {
        fun create(
            club: Club,
            title: String,
            content: String?,
            location: String?,
            cardinal: Int,
            start: LocalDateTime,
            end: LocalDateTime,
            user: User?,
        ): Event {
            require(title.isNotBlank()) { "제목은 필수입니다" }
            require(!end.isBefore(start)) { "종료 시간은 시작 시간 이후여야 합니다" }
            return Event(
                club = club,
                title = title,
                content = content,
                location = location,
                cardinal = cardinal,
                start = start,
                end = end,
                user = user,
            )
        }
    }
}
