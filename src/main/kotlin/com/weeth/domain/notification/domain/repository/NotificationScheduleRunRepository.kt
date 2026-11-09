package com.weeth.domain.notification.domain.repository

import com.weeth.domain.notification.domain.entity.NotificationScheduleRun
import jakarta.persistence.LockModeType
import jakarta.persistence.QueryHint
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import org.springframework.data.repository.query.Param
import java.time.LocalDate

interface NotificationScheduleRunRepository : JpaRepository<NotificationScheduleRun, Long> {
    @Modifying
    @Query(
        value = """INSERT IGNORE INTO notification_schedule_run
        (club_id, local_date, job_type, status, created_at, modified_at)
        VALUES (:clubId, :date, 'SESSION_DAY_REMINDER', 'PENDING', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))""",
        nativeQuery = true,
    )
    fun createIfAbsent(
        @Param("clubId") clubId: Long,
        @Param("date") date: LocalDate,
    ): Int

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(QueryHint(name = "jakarta.persistence.lock.timeout", value = "2000"))
    @Query(
        "SELECT r FROM NotificationScheduleRun r WHERE r.clubId = :clubId AND r.localDate = :date AND r.jobType = com.weeth.domain.notification.domain.enums.NotificationType.SESSION_DAY_REMINDER",
    )
    fun findWithLock(
        @Param("clubId") clubId: Long,
        @Param("date") date: LocalDate,
    ): NotificationScheduleRun?
}
