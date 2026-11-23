package com.weeth.domain.notification.application.usecase.query

import com.weeth.domain.account.domain.repository.AccountSettingReader
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.notification.application.dto.response.NotificationPreferencesResponse
import com.weeth.domain.notification.application.mapper.NotificationPreferenceMapper
import com.weeth.domain.notification.domain.repository.NotificationPreferenceRepository
import com.weeth.domain.notification.domain.repository.NotificationSettingRepository
import com.weeth.domain.notification.domain.service.NotificationPreferencePolicy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetNotificationPreferenceQueryService(
    private val clubMemberPolicy: ClubMemberPolicy,
    private val accountSettingReader: AccountSettingReader,
    private val settingRepository: NotificationSettingRepository,
    private val preferenceRepository: NotificationPreferenceRepository,
    private val preferencePolicy: NotificationPreferencePolicy,
    private val mapper: NotificationPreferenceMapper,
) {
    fun find(
        clubId: Long,
        userId: Long,
    ): NotificationPreferencesResponse {
        val member = clubMemberPolicy.getActiveMember(clubId, userId)
        val available =
            preferencePolicy.availableCategories(
                member.club.warningEnabled,
                accountSettingReader.isVisibleToMembers(clubId),
            )
        val masterEnabled = settingRepository.findByUserIdAndClubId(userId, clubId)?.masterEnabled ?: true
        return mapper.toResponse(
            masterEnabled,
            preferenceRepository.findAllByUserIdAndClubId(userId, clubId),
            available,
        )
    }
}
