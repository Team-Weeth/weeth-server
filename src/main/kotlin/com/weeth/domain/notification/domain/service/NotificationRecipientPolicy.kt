package com.weeth.domain.notification.domain.service

import com.weeth.domain.account.domain.repository.AccountSettingReader
import com.weeth.domain.club.domain.repository.ClubMemberReader
import com.weeth.domain.club.domain.repository.ClubReader
import com.weeth.domain.notification.domain.enums.NotificationCategory
import com.weeth.domain.notification.domain.repository.NotificationPreferenceRepository
import com.weeth.domain.notification.domain.repository.NotificationSettingRepository
import org.springframework.stereotype.Service

@Service
class NotificationRecipientPolicy(
    private val settingRepository: NotificationSettingRepository,
    private val preferenceRepository: NotificationPreferenceRepository,
    private val clubMemberReader: ClubMemberReader,
    private val clubReader: ClubReader,
    private val accountSettingReader: AccountSettingReader,
) {
    fun filterEligibleUserIds(
        clubId: Long,
        category: NotificationCategory,
        candidateUserIds: List<Long>,
    ): List<Long> {
        val candidates = candidateUserIds.distinct()
        if (candidates.isEmpty()) return emptyList()
        val available =
            when (category) {
                NotificationCategory.NOTICE, NotificationCategory.SCHEDULE -> true
                NotificationCategory.DUES -> accountSettingReader.isVisibleToMembers(clubId)
                NotificationCategory.WARNING -> clubReader.findByIdOrNull(clubId)?.warningEnabled ?: false
            }
        if (!available) return emptyList()

        val activeUserIds =
            clubMemberReader
                .findAllByClubIdAndUserIds(clubId, candidates)
                .filter { it.isActive() && it.user.isActive() }
                .map { it.user.id }
                .toSet()
        val activeCandidates = candidates.filter { it in activeUserIds }
        if (activeCandidates.isEmpty()) return emptyList()

        val settings = settingRepository.findAllByClubIdAndUserIdIn(clubId, activeCandidates).associateBy { it.userId }
        val preferences =
            preferenceRepository
                .findAllByClubIdAndCategoryAndUserIdIn(clubId, category, activeCandidates)
                .associateBy { it.userId }
        return activeCandidates.filter { userId ->
            category.isEffectivelyEnabled(
                settings[userId]?.masterEnabled ?: true,
                preferences[userId]?.enabled ?: category.defaultEnabled,
                available,
            )
        }
    }
}
