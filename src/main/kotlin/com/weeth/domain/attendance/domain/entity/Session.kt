package com.weeth.domain.attendance.domain.entity

import com.weeth.domain.attendance.domain.entity.enums.SessionStatus
import com.weeth.domain.user.domain.entity.User
import com.weeth.global.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "meeting")
class Session(
    var title: String,
    @Column(length = 500)
    var content: String? = null,
    var location: String? = null,
    var cardinal: Int,
    var start: LocalDateTime,
    var end: LocalDateTime,
    var code: Int,
    @Enumerated(EnumType.STRING)
    var status: SessionStatus = SessionStatus.OPEN,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    var user: User? = null,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    fun close() {
        check(status == SessionStatus.OPEN) { "이미 종료된 세션입니다" }
        status = SessionStatus.CLOSED
    }

    fun updateInfo(
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

    fun isCodeMatch(code: Int): Boolean = this.code == code

    fun isInProgress(now: LocalDateTime): Boolean = !now.isBefore(start) && !now.isAfter(end)

    companion object {
        fun create(
            title: String,
            content: String?,
            location: String?,
            cardinal: Int,
            start: LocalDateTime,
            end: LocalDateTime,
            user: User?,
        ): Session {
            require(title.isNotBlank()) { "제목은 필수입니다" }
            require(!end.isBefore(start)) { "종료 시간은 시작 시간 이후여야 합니다" }
            return Session(
                title = title,
                content = content,
                location = location,
                cardinal = cardinal,
                start = start,
                end = end,
                code = generateCode(),
                user = user,
            )
        }

        private fun generateCode(): Int = (1000..9999).random()
    }
}
