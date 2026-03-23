package com.weeth.domain.session.domain.entity

import com.weeth.domain.club.domain.entity.Club
import com.weeth.domain.session.domain.enums.SessionStatus
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
import java.security.SecureRandom
import java.time.LocalDateTime
import kotlin.random.asKotlinRandom

@Entity
@Table(name = "meeting") // 테이블명 Session으로 수정
class Session(
    club: Club,
    title: String,
    content: String? = null,
    location: String? = null,
    cardinal: Int,
    start: LocalDateTime,
    end: LocalDateTime,
    code: Int,
    status: SessionStatus = SessionStatus.OPEN,
    user: User? = null,
) : BaseEntity() {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    var club: Club = club
        private set

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L
        private set

    var title: String = title
        private set

    @Column(length = 500)
    var content: String? = content
        private set

    var location: String? = location
        private set

    var cardinal: Int = cardinal
        private set

    var start: LocalDateTime = start
        private set

    var end: LocalDateTime = end
        private set

    var code: Int = code
        private set

    @Enumerated(EnumType.STRING)
    var status: SessionStatus = status
        private set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    var user: User? = user
        private set

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

    fun isCheckInAllowed(now: LocalDateTime): Boolean {
        val from = start.minusMinutes(10)
        val to = end.plusMinutes(10)
        return !now.isBefore(from) && !now.isAfter(to)
    }

    companion object {
        private val secureRandom = SecureRandom().asKotlinRandom()

        fun create(
            club: Club,
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
                club = club,
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

        private fun generateCode(): Int = (100000..999999).random(secureRandom)
    }
}
