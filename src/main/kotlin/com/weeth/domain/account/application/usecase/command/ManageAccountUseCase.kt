package com.weeth.domain.account.application.usecase.command

import com.weeth.domain.account.domain.entity.AccountSetting
import com.weeth.domain.account.domain.repository.AccountSettingRepository
import com.weeth.domain.club.domain.service.ClubPermissionPolicy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ManageAccountUseCase(
    private val accountSettingRepository: AccountSettingRepository,
    private val clubPermissionPolicy: ClubPermissionPolicy,
) {
    /** 회비 기능 전체의 club 단위 부원 공개 여부를 설정한다. 설정 행이 없으면 생성한다. */
    @Transactional
    fun updateMemberVisibility(
        clubId: Long,
        visible: Boolean,
        userId: Long,
    ) {
        clubPermissionPolicy.requireAdmin(clubId, userId)
        val setting =
            accountSettingRepository.findByClubId(clubId)
                ?: AccountSetting.createDefault(clubId)

        if (visible) {
            setting.showToMembers()
        } else {
            setting.hideFromMembers()
        }

        accountSettingRepository.save(setting)
    }
}
