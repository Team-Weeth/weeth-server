package com.weeth.domain.notification.application.usecase.command

import com.weeth.domain.notification.domain.entity.NotificationScheduleRun
import com.weeth.domain.notification.domain.enums.NotificationReferenceType
import com.weeth.domain.notification.domain.enums.NotificationType
import com.weeth.domain.notification.domain.repository.NotificationScheduleRunRepository
import com.weeth.domain.notification.domain.service.SessionDayReminderPolicy
import com.weeth.domain.notification.domain.vo.NotificationRequest
import com.weeth.domain.notification.domain.vo.NotificationScheduleClaim
import com.weeth.global.common.id.TsidBase62Encoder
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

@Service
class GenerateSessionDayReminderUseCase(
    private val repository: NotificationScheduleRunRepository,
    private val policy: SessionDayReminderPolicy,
    private val createNotification: CreateNotificationUseCase,
    private val clock: Clock,
    @Value("\${notification.dispatch.enabled:false}") private val durableEnabled: Boolean = false,
) {
    @Transactional
    fun claim(clubId: Long): NotificationScheduleClaim? {
        val now = now()
        if (!durableEnabled || !policy.isWindow(now)) return null
        val candidate = NotificationScheduleRun.create(clubId, now.toLocalDate())
        repository.createIfAbsent(candidate.clubId, candidate.localDate)
        val run = checkNotNull(repository.findWithLock(clubId, candidate.localDate))
        val token = UUID.randomUUID().toString()
        if (!run.claim(token, now)) return null
        return NotificationScheduleClaim(run.id, run.clubId, run.localDate, token)
    }

    @Transactional
    fun generate(claim: NotificationScheduleClaim): Boolean {
        if (!durableEnabled) return false
        val run = repository.findWithLock(claim.clubId, claim.localDate) ?: return false
        val now = now()
        if (!policy.isWindow(now) || claim.localDate != now.toLocalDate()) {
            run.expire(claim.token)
            return false
        }
        if (run.id != claim.runId || !run.owns(claim.token, now)) return false
        val sessions = policy.sessionsFor(claim.clubId, claim.localDate, now)
        val clubId = TsidBase62Encoder.encode(claim.clubId)
        val targetPath = "/clubs/$clubId/schedules"
        policy.audiences(claim.clubId, sessions).forEach { (userId, visible) ->
            check(policy.isWindow(now())) { "정오 안내 생성 창이 종료되었습니다." }
            createNotification.execute(
                NotificationRequest(
                    eventId = "SESSION_DAY_REMINDER:${claim.clubId}:$userId:${claim.localDate}",
                    type = NotificationType.SESSION_DAY_REMINDER,
                    variables = mapOf("sessionSummary" to policy.summary(visible)),
                    targetPath = targetPath,
                    clubId = claim.clubId,
                    referenceType = NotificationReferenceType.SESSION,
                    referenceId = visible.first().id,
                    data =
                        mapOf(
                            "type" to NotificationType.SESSION_DAY_REMINDER.name,
                            "referenceType" to NotificationReferenceType.SESSION.name,
                            "referenceId" to visible.first().id.toString(),
                            "clubId" to clubId,
                            "targetPath" to targetPath,
                        ),
                    sourceSessionIds = visible.map { it.id },
                    expiresAt = policy.expiresAt(claim.localDate),
                ),
                listOf(userId),
            )
        }
        val completedAt = now()
        check(policy.isWindow(completedAt)) { "정오 안내 생성 창이 종료되었습니다." }
        run.complete(claim.token, completedAt)
        return true
    }

    @Transactional
    fun expire(claim: NotificationScheduleClaim) {
        if (claim.localDate != now().toLocalDate() || !policy.isWindow(now())) {
            repository.findWithLock(claim.clubId, claim.localDate)?.expire(claim.token)
        }
    }

    private fun now(): LocalDateTime = LocalDateTime.ofInstant(clock.instant(), ZoneId.of("Asia/Seoul"))
}
