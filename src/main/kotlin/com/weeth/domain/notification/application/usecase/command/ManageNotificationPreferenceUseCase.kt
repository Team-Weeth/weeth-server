package com.weeth.domain.notification.application.usecase.command

import com.weeth.domain.account.domain.repository.AccountSettingReader
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.notification.application.dto.request.UpdateNotificationPreferencesRequest
import com.weeth.domain.notification.application.exception.InvalidNotificationPreferenceException
import com.weeth.domain.notification.application.exception.NotificationErrorCode
import com.weeth.domain.notification.domain.entity.NotificationPreference
import com.weeth.domain.notification.domain.entity.NotificationSetting
import com.weeth.domain.notification.domain.enums.NotificationPreferenceUpdateFailure
import com.weeth.domain.notification.domain.repository.NotificationPreferenceRepository
import com.weeth.domain.notification.domain.repository.NotificationSettingRepository
import com.weeth.domain.notification.domain.service.NotificationPreferencePolicy
import org.springframework.dao.PessimisticLockingFailureException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ManageNotificationPreferenceUseCase(
    private val clubMemberPolicy: ClubMemberPolicy,
    private val accountSettingReader: AccountSettingReader,
    private val settingRepository: NotificationSettingRepository,
    private val preferenceRepository: NotificationPreferenceRepository,
    private val preferencePolicy: NotificationPreferencePolicy,
) {
    @Transactional
    fun update(
        clubId: Long,
        userId: Long,
        request: UpdateNotificationPreferencesRequest,
    ) {
        // 설정 행이 없는 최초 PATCH도 같은 회원 행을 먼저 잠가 직렬화한다.
        val member =
            try {
                clubMemberPolicy.getActiveMemberWithLock(clubId, userId)
            } catch (_: PessimisticLockingFailureException) {
                throw InvalidNotificationPreferenceException(NotificationErrorCode.NOTIFICATION_PREFERENCE_LOCK_TIMEOUT)
            }
        val changes =
            request.preferences.orEmpty().map { item ->
                val category =
                    item?.category
                        ?: throw InvalidNotificationPreferenceException(
                            NotificationErrorCode.NOTIFICATION_PREFERENCE_INVALID,
                        )
                val enabled =
                    item.enabled
                        ?: throw InvalidNotificationPreferenceException(
                            NotificationErrorCode.NOTIFICATION_PREFERENCE_INVALID,
                        )
                category to enabled
            }
        val available =
            preferencePolicy.availableCategories(
                member.club.warningEnabled,
                accountSettingReader.isVisibleToMembers(clubId),
            )
        val existingSetting = settingRepository.findByUserIdAndClubId(userId, clubId)
        val failure =
            preferencePolicy
                .validateUpdate(
                    existingSetting?.masterEnabled ?: true,
                    request.masterEnabled,
                    changes.map { it.first },
                    available,
                )
        if (failure != null) {
            val errorCode =
                when (failure) {
                    NotificationPreferenceUpdateFailure.DUPLICATE_CATEGORY -> {
                        NotificationErrorCode.NOTIFICATION_DUPLICATE_CATEGORY
                    }

                    NotificationPreferenceUpdateFailure.MASTER_DISABLED -> {
                        NotificationErrorCode.NOTIFICATION_MASTER_DISABLED
                    }

                    NotificationPreferenceUpdateFailure.CATEGORY_UNAVAILABLE -> {
                        NotificationErrorCode.NOTIFICATION_CATEGORY_UNAVAILABLE
                    }
                }
            throw InvalidNotificationPreferenceException(errorCode)
        }
        if (request.masterEnabled == null && changes.isEmpty()) return

        val setting = existingSetting ?: NotificationSetting.create(userId, clubId)
        setting.updateMaster(request.masterEnabled)
        settingRepository.save(setting)
        if (changes.isEmpty()) return

        val existing = preferenceRepository.findAllByUserIdAndClubId(userId, clubId).associateBy { it.category }
        val updated =
            changes.map { (category, enabled) ->
                existing[category]?.apply { updateEnabled(enabled) }
                    ?: NotificationPreference.create(userId, clubId, category, enabled)
            }
        preferenceRepository.saveAll(updated)
    }
}
