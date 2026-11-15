package com.weeth.domain.notification.domain.service

import com.weeth.domain.club.domain.enums.MemberStatus
import com.weeth.domain.club.domain.repository.ClubMemberCardinalReader
import com.weeth.domain.notification.domain.entity.NotificationDelivery
import com.weeth.domain.notification.domain.enums.NotificationChannel
import com.weeth.domain.notification.domain.enums.NotificationType
import com.weeth.domain.notification.domain.vo.NotificationDeliveryMessage
import com.weeth.domain.notification.domain.vo.NotificationRenderingResult
import com.weeth.domain.notification.domain.vo.RenderedNotification
import com.weeth.domain.session.domain.entity.Session
import com.weeth.domain.session.domain.repository.SessionReader
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Service
class SessionDayReminderPolicy(
    private val sessionReader: SessionReader,
    private val memberCardinalReader: ClubMemberCardinalReader,
    private val renderer: NotificationTemplateRenderer,
) {
    fun isWindow(now: LocalDateTime): Boolean = now.toLocalTime() >= NOON && now.toLocalTime() < CUTOFF

    fun expiresAt(date: LocalDate): LocalDateTime = date.atTime(CUTOFF)

    fun sessionsFor(
        clubId: Long,
        date: LocalDate,
        now: LocalDateTime,
    ): List<Session> =
        sessionReader.findDayReminderSessions(
            clubId,
            date.atStartOfDay(),
            date.plusDays(1).atStartOfDay(),
            now,
            date.atTime(NOON),
        )

    fun audiences(
        clubId: Long,
        sessions: List<Session>,
    ): Map<Long, List<Session>> =
        sessions
            .groupBy { it.cardinal }
            .flatMap { (cardinal, visibleSessions) ->
                memberCardinalReader
                    .findAllByClubIdAndCardinalNumber(clubId, cardinal, MemberStatus.ACTIVE)
                    .filter { it.clubMember.user.isActive() }
                    .flatMap { member -> visibleSessions.map { member.clubMember.user.id to it } }
            }.groupBy({ it.first }, { it.second })
            .mapValues { (_, visible) ->
                visible.distinctBy { it.id }.sortedWith(compareBy({ it.start }, { it.id }))
            }

    fun summary(sessions: List<Session>): String {
        require(sessions.isNotEmpty())
        val lines =
            sessions
                .sortedWith(
                    compareBy({ it.start }, { it.id }),
                ).map { "${it.start.format(TIME)} ${it.title}" }
        val text = lines.joinToString(" · ")
        if (text.length <= SUMMARY_LIMIT) return text
        val suffix = if (lines.size > 1) " 외 ${lines.size - 1}개" else ""
        return lines.first().take(SUMMARY_LIMIT - suffix.length - 1) + "…" + suffix
    }

    fun prepareMessage(
        delivery: NotificationDelivery,
        now: LocalDateTime,
    ): NotificationDeliveryMessage {
        if (delivery.type != NotificationType.SESSION_DAY_REMINDER) {
            return NotificationDeliveryMessage.Ready(
                RenderedNotification(delivery.title, delivery.body, delivery.templateVersion),
            )
        }
        val expires = delivery.expiresAt ?: return NotificationDeliveryMessage.Skip("REMINDER_INVALID_SNAPSHOT")
        if (!now.isBefore(expires) || expires.toLocalDate() != now.toLocalDate()) {
            return NotificationDeliveryMessage.Skip("REMINDER_EXPIRED")
        }
        val snapshot = delivery.sourceSessionIds.orEmpty().toSet()
        if (snapshot.isEmpty()) return NotificationDeliveryMessage.Skip("REMINDER_INVALID_SNAPSHOT")
        val cardinals =
            memberCardinalReader
                .findCardinalNumbersByClubIdAndUserId(
                    delivery.clubId,
                    delivery.userId,
                ).toSet()
        val visible =
            sessionReader
                .findDayReminderSessionsByIds(
                    delivery.clubId,
                    snapshot,
                    expires.toLocalDate().atStartOfDay(),
                    expires.toLocalDate().plusDays(1).atStartOfDay(),
                    now,
                    expires.toLocalDate().atTime(NOON),
                ).filter { it.cardinal in cardinals }
        if (visible.isEmpty()) return NotificationDeliveryMessage.Skip("REMINDER_NO_VALID_SESSION")
        return when (
            val rendered =
                renderer.render(
                    delivery.type,
                    NotificationChannel.PUSH,
                    mapOf("sessionSummary" to summary(visible)),
                    delivery.templateVersion,
                )
        ) {
            is NotificationRenderingResult.Success -> {
                NotificationDeliveryMessage.Ready(rendered.message)
            }

            is NotificationRenderingResult.Failure -> {
                NotificationDeliveryMessage.Skip(
                    "REMINDER_TEMPLATE_${rendered.reason.name}",
                )
            }
        }
    }

    private companion object {
        val NOON: LocalTime = LocalTime.NOON
        val CUTOFF: LocalTime = LocalTime.of(12, 30)
        val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        const val SUMMARY_LIMIT = 240
    }
}
