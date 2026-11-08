package com.weeth.domain.notification.domain.entity

import com.weeth.global.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "notification_event")
class NotificationEvent private constructor(
    eventId: String,
    clubId: Long,
) : BaseEntity() {
    @Id
    @Column(name = "event_id", length = 150)
    var eventId: String = eventId
        private set

    @Column(name = "club_id", nullable = false)
    var clubId: Long = clubId
        private set

    companion object {
        fun create(
            eventId: String,
            clubId: Long,
        ): NotificationEvent {
            require(eventId.isNotBlank() && eventId.length <= 150 && clubId > 0)
            return NotificationEvent(eventId, clubId)
        }
    }
}
