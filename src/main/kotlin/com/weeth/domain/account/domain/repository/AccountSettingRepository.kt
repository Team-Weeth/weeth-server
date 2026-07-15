package com.weeth.domain.account.domain.repository

import com.weeth.domain.account.domain.entity.AccountSetting
import org.springframework.data.jpa.repository.JpaRepository

interface AccountSettingRepository :
    JpaRepository<AccountSetting, Long>,
    AccountSettingReader {
    fun findByClubId(clubId: Long): AccountSetting?

    // 설정 행이 없으면 아직 공개 설정을 한 적 없는 것이므로 미공개로 간주한다.
    override fun isVisibleToMembers(clubId: Long): Boolean = findByClubId(clubId)?.memberVisible ?: false
}
