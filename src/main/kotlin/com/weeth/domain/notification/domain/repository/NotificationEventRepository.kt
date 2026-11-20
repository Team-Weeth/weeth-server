package com.weeth.domain.notification.domain.repository

import com.weeth.domain.notification.domain.entity.NotificationEvent
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface NotificationEventRepository : JpaRepository<NotificationEvent, String> {
    // 필수 값은 factory에서 검증한다. INSERT IGNORE는 동일 PK의 동시 선점을 원자적으로 처리한다.
    @Modifying
    @Query(
        value = """INSERT IGNORE INTO notification_event (event_id, club_id, created_at, modified_at)
            VALUES (:eventId, :clubId, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))""",
        nativeQuery = true,
    )
    fun claimIfAbsent(
        @Param("eventId") eventId: String,
        @Param("clubId") clubId: Long,
    ): Int
}
