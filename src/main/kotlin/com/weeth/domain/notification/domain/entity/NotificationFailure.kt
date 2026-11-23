package com.weeth.domain.notification.domain.entity

import com.weeth.domain.notification.domain.enums.NotificationChannel
import com.weeth.domain.notification.domain.enums.NotificationTemplateFailureReason
import com.weeth.domain.notification.domain.enums.NotificationType
import com.weeth.domain.notification.domain.vo.NotificationRenderingResult
import com.weeth.domain.notification.domain.vo.NotificationRequest
import com.weeth.global.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

@Entity
@Table(
    name = "notification_failure",
    indexes = [Index(name = "idx_notification_failure_event_created", columnList = "event_id, created_at")],
)
class NotificationFailure private constructor(
    request: NotificationRequest,
    channel: NotificationChannel,
    failure: NotificationRenderingResult.Failure,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_failure_id")
    var id: Long = 0L
        private set

    @Column(name = "event_id", nullable = false, length = 150)
    var eventId: String = request.eventId
        private set

    @Column(name = "club_id", nullable = false)
    var clubId: Long = request.clubId
        private set

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    var type: NotificationType = request.type
        private set

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    var channel: NotificationChannel = channel
        private set

    @Column(name = "template_version")
    var templateVersion: Int? = failure.templateVersion
        private set

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 40)
    var reason: NotificationTemplateFailureReason = failure.reason
        private set

    @Column(name = "missing_variables", nullable = false, length = 1000)
    var missingVariables: String =
        failure.missingVariables
            .sorted()
            .joinToString(",")
            .take(1000)
        private set

    companion object {
        fun create(
            request: NotificationRequest,
            channel: NotificationChannel,
            failure: NotificationRenderingResult.Failure,
        ): NotificationFailure = NotificationFailure(request, channel, failure)
    }
}
