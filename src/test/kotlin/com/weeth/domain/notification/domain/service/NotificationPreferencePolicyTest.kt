package com.weeth.domain.notification.domain.service

import com.weeth.domain.notification.domain.entity.NotificationPreference
import com.weeth.domain.notification.domain.entity.NotificationSetting
import com.weeth.domain.notification.domain.enums.NotificationCategory
import com.weeth.domain.notification.domain.enums.NotificationPreferenceUpdateFailure
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class NotificationPreferencePolicyTest :
    StringSpec({
        val policy = NotificationPreferencePolicy()
        val allCategories = NotificationCategory.entries.toSet()

        "마스터 변경은 저장된 개별 선택값을 바꾸지 않는다" {
            val setting = NotificationSetting.create(userId = 10L, clubId = 1L)
            val preference = NotificationPreference.create(10L, 1L, NotificationCategory.NOTICE, false)
            setting.updateMaster(false)
            setting.masterEnabled shouldBe false
            preference.enabled shouldBe false
            setting.updateMaster(true)
            setting.updateMaster(null)
            setting.masterEnabled shouldBe true
            preference.enabled shouldBe false
        }

        "마스터 OFF에서 개별 변경은 거부하지만 ON과 함께 변경하면 허용한다" {
            policy.validateUpdate(false, null, listOf(NotificationCategory.NOTICE), allCategories) shouldBe
                NotificationPreferenceUpdateFailure.MASTER_DISABLED
            policy.validateUpdate(false, true, listOf(NotificationCategory.NOTICE), allCategories) shouldBe null
            policy.validateUpdate(true, false, listOf(NotificationCategory.NOTICE), allCategories) shouldBe
                NotificationPreferenceUpdateFailure.MASTER_DISABLED
            policy.validateUpdate(false, null, emptyList(), allCategories) shouldBe null
        }

        "중복 카테고리와 기능 사용 불가 카테고리는 거부한다" {
            policy.validateUpdate(
                true,
                null,
                listOf(NotificationCategory.NOTICE, NotificationCategory.NOTICE),
                allCategories,
            ) shouldBe
                NotificationPreferenceUpdateFailure.DUPLICATE_CATEGORY
            policy.validateUpdate(
                true,
                null,
                listOf(NotificationCategory.DUES),
                setOf(NotificationCategory.NOTICE),
            ) shouldBe
                NotificationPreferenceUpdateFailure.CATEGORY_UNAVAILABLE
        }

        "경고와 회비 기능 상태로 카테고리 가용성을 결정한다" {
            policy.availableCategories(warningEnabled = false, duesEnabled = false) shouldBe
                setOf(NotificationCategory.NOTICE, NotificationCategory.SCHEDULE)
            policy.availableCategories(warningEnabled = true, duesEnabled = true) shouldBe allCategories
        }
    })
