package com.weeth.domain.notification.application.dispatch

import com.weeth.domain.notification.application.usecase.command.GenerateSessionDayReminderUseCase
import com.weeth.domain.notification.domain.service.SessionDayReminderPolicy
import com.weeth.domain.session.domain.repository.SessionReader
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.data.domain.PageRequest
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

@Component
@ConditionalOnProperty(prefix = "notification.dispatch", name = ["enabled"], havingValue = "true")
class SessionDayReminderScheduler(
    private val generator: GenerateSessionDayReminderUseCase,
    private val sessions: SessionReader,
    private val policy: SessionDayReminderPolicy,
    private val clock: Clock,
    @Value("\${notification.day-reminder.enabled:false}") private val enabled: Boolean = false,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 * * * * *", zone = "Asia/Seoul", scheduler = "notificationMaintenanceScheduler")
    fun runOnce() {
        if (!enabled) return
        var afterClubId = 0L
        while (true) {
            val now = LocalDateTime.ofInstant(clock.instant(), ZoneId.of("Asia/Seoul"))
            if (!policy.isWindow(now)) return
            val date = now.toLocalDate()
            val clubIds =
                sessions.findDayReminderClubIds(
                    date.atStartOfDay(),
                    date.plusDays(1).atStartOfDay(),
                    now,
                    date.atTime(LocalTime.NOON),
                    afterClubId,
                    PageRequest.of(0, 100),
                )
            if (clubIds.isEmpty()) return
            clubIds.forEach { clubId ->
                try {
                    val claim = generator.claim(clubId) ?: return@forEach
                    try {
                        generator.generate(claim)
                    } catch (exception: Exception) {
                        log.warn("정오 안내 생성 실패. clubId={}, exceptionType={}", clubId, exception.javaClass.simpleName)
                        generator.expire(claim)
                    }
                } catch (_: Exception) {
                    log.warn("정오 안내 선점·복구 실패. clubId={}", clubId)
                }
            }
            afterClubId = clubIds.last()
        }
    }
}
