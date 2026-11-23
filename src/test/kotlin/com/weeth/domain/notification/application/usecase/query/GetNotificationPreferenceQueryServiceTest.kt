package com.weeth.domain.notification.application.usecase.query

import com.weeth.domain.account.domain.repository.AccountSettingReader
import com.weeth.domain.club.application.exception.ClubMemberNotFoundException
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.club.fixture.ClubMemberTestFixture
import com.weeth.domain.club.fixture.ClubTestFixture
import com.weeth.domain.notification.application.mapper.NotificationPreferenceMapper
import com.weeth.domain.notification.domain.entity.NotificationPreference
import com.weeth.domain.notification.domain.entity.NotificationSetting
import com.weeth.domain.notification.domain.enums.NotificationCategory
import com.weeth.domain.notification.domain.repository.NotificationPreferenceRepository
import com.weeth.domain.notification.domain.repository.NotificationSettingRepository
import com.weeth.domain.notification.domain.service.NotificationPreferencePolicy
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class GetNotificationPreferenceQueryServiceTest :
    DescribeSpec({
        val members = mockk<ClubMemberPolicy>()
        val accounts = mockk<AccountSettingReader>()
        val settings = mockk<NotificationSettingRepository>()
        val preferences = mockk<NotificationPreferenceRepository>()
        val service =
            GetNotificationPreferenceQueryService(
                members,
                accounts,
                settings,
                preferences,
                NotificationPreferencePolicy(),
                NotificationPreferenceMapper(),
            )

        beforeTest {
            clearMocks(members, accounts, settings, preferences)
            every { members.getActiveMember(1L, 10L) } returns
                ClubMemberTestFixture.createActiveMember(club = ClubTestFixture.createClub(1L))
            every { accounts.isVisibleToMembers(1L) } returns false
            every { settings.findByUserIdAndClubId(10L, 1L) } returns null
            every { preferences.findAllByUserIdAndClubId(10L, 1L) } returns emptyList()
        }

        describe("동아리별 설정 조회") {
            it("설정 행이 없으면 모두 ON이며 기능 미사용 카테고리는 유효 수신만 false다") {
                val response = service.find(1L, 10L)
                response.masterEnabled shouldBe true
                response.preferences.map { it.category } shouldBe NotificationCategory.entries
                response.preferences.all { it.enabled && it.defaultEnabled } shouldBe true
                response.preferences.filter { it.available }.map { it.category } shouldBe
                    listOf(NotificationCategory.NOTICE, NotificationCategory.SCHEDULE)
                response.preferences.filter { it.effectiveEnabled }.map { it.category } shouldBe
                    listOf(NotificationCategory.NOTICE, NotificationCategory.SCHEDULE)
                verify(exactly = 0) {
                    settings.save(any())
                    preferences.save(any())
                }
            }

            it("마스터 OFF여도 개별 선택값은 그대로 반환한다") {
                every { settings.findByUserIdAndClubId(10L, 1L) } returns
                    NotificationSetting.create(10L, 1L).apply { updateMaster(false) }
                every { preferences.findAllByUserIdAndClubId(10L, 1L) } returns
                    listOf(NotificationPreference.create(10L, 1L, NotificationCategory.NOTICE, false))
                val response = service.find(1L, 10L)
                response.masterEnabled shouldBe false
                response.preferences.first { it.category == NotificationCategory.NOTICE }.enabled shouldBe false
                response.preferences.first { it.category == NotificationCategory.SCHEDULE }.enabled shouldBe true
                response.preferences.any { it.effectiveEnabled } shouldBe false
            }

            it("비회원은 설정을 읽기 전에 거부한다") {
                every { members.getActiveMember(1L, 10L) } throws ClubMemberNotFoundException()
                shouldThrow<ClubMemberNotFoundException> { service.find(1L, 10L) }
                verify(exactly = 0) {
                    settings.findByUserIdAndClubId(any(), any())
                    preferences.findAllByUserIdAndClubId(any(), any())
                }
            }
        }
    })
