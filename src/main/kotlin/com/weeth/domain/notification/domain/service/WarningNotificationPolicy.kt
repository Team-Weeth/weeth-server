package com.weeth.domain.notification.domain.service

import com.weeth.domain.notification.domain.enums.NotificationType
import com.weeth.domain.penalty.domain.repository.PenaltyReader
import org.springframework.stereotype.Service

@Service
class WarningNotificationPolicy(
    private val penaltyReader: PenaltyReader,
) {
    fun filterEligibleUserIds(
        type: NotificationType,
        clubId: Long,
        referenceId: Long?,
        candidates: List<Long>,
    ): List<Long> {
        if (type != NotificationType.WARNING_ASSIGNED) return candidates
        if (referenceId == null || candidates.isEmpty()) return emptyList()
        val penalty = penaltyReader.findByIdWithNotificationDetails(referenceId) ?: return emptyList()
        if (penalty.clubMember.club.id != clubId || !penalty.isActiveWarning()) return emptyList()
        return candidates.filter { it == penalty.clubMember.user.id }.distinct()
    }
}
