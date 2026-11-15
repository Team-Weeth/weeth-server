package com.weeth.domain.notification.domain.service

import com.weeth.domain.account.domain.repository.AccountPaymentTargetReader
import com.weeth.domain.notification.domain.enums.NotificationType
import org.springframework.stereotype.Service

@Service
class DuesNotificationPolicy(
    private val targetReader: AccountPaymentTargetReader,
) {
    fun filterEligibleUserIds(
        type: NotificationType,
        clubId: Long,
        referenceId: Long?,
        candidates: List<Long>,
        accountId: Long?,
    ): List<Long> {
        if (type != NotificationType.DUES_OBLIGATION_CREATED) return candidates
        if (referenceId == null || accountId == null || candidates.isEmpty()) return emptyList()
        val target = targetReader.findByIdWithObligationDetails(referenceId) ?: return emptyList()
        if (target.account.id != accountId || target.account.club.id != clubId ||
            target.outstandingObligation() == null
        ) {
            return emptyList()
        }
        return candidates.filter { it == target.clubMember.user.id }.distinct()
    }
}
