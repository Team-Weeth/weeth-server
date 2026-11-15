package com.weeth.domain.notification.domain.entity

import com.weeth.domain.notification.domain.enums.NotificationScheduleStatus
import com.weeth.domain.notification.domain.enums.NotificationType
import com.weeth.global.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(
    name = "notification_schedule_run",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_schedule_run_club_date_type", columnNames = ["club_id", "local_date", "job_type"]),
    ],
)
class NotificationScheduleRun private constructor(
    clubId: Long,
    localDate: LocalDate,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_schedule_run_id")
    var id: Long = 0
        private set

    @Column(name = "club_id", nullable = false)
    var clubId: Long = clubId
        private set

    @Column(name = "local_date", nullable = false)
    var localDate: LocalDate = localDate
        private set

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "job_type", nullable = false, length = 30)
    var jobType: NotificationType = NotificationType.SESSION_DAY_REMINDER
        private set

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    var status: NotificationScheduleStatus = NotificationScheduleStatus.PENDING
        private set

    @Column(name = "claim_token", length = 36)
    var claimToken: String? = null
        private set

    @Column(name = "lease_until")
    var leaseUntil: LocalDateTime? = null
        private set

    fun claim(
        token: String,
        now: LocalDateTime,
    ): Boolean {
        if (status in setOf(NotificationScheduleStatus.COMPLETED, NotificationScheduleStatus.EXPIRED)) return false
        if (status == NotificationScheduleStatus.PROCESSING && leaseUntil?.isAfter(now) == true) return false
        require(token.isNotBlank())
        status = NotificationScheduleStatus.PROCESSING
        claimToken = token
        leaseUntil = now.plusSeconds(60)
        return true
    }

    fun owns(
        token: String,
        now: LocalDateTime,
    ): Boolean =
        status == NotificationScheduleStatus.PROCESSING && claimToken == token && leaseUntil?.isAfter(now) == true

    fun complete(
        token: String,
        now: LocalDateTime,
    ) {
        check(owns(token, now)) { "정오 안내 선점이 만료되었습니다." }
        status = NotificationScheduleStatus.COMPLETED
        claimToken = null
        leaseUntil = null
    }

    fun expire(token: String) {
        if (status != NotificationScheduleStatus.PROCESSING || claimToken != token) return
        status = NotificationScheduleStatus.EXPIRED
        claimToken = null
        leaseUntil = null
    }

    companion object {
        fun create(
            clubId: Long,
            localDate: LocalDate,
        ): NotificationScheduleRun {
            require(clubId > 0)
            return NotificationScheduleRun(clubId, localDate)
        }
    }
}
