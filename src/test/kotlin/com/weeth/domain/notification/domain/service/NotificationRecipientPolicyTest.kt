package com.weeth.domain.notification.domain.service

import com.weeth.domain.account.domain.repository.AccountSettingReader
import com.weeth.domain.club.domain.repository.ClubMemberReader
import com.weeth.domain.club.domain.repository.ClubReader
import com.weeth.domain.club.fixture.ClubMemberTestFixture
import com.weeth.domain.club.fixture.ClubTestFixture
import com.weeth.domain.notification.domain.entity.NotificationPreference
import com.weeth.domain.notification.domain.entity.NotificationSetting
import com.weeth.domain.notification.domain.enums.NotificationCategory
import com.weeth.domain.notification.domain.repository.NotificationPreferenceRepository
import com.weeth.domain.notification.domain.repository.NotificationSettingRepository
import com.weeth.domain.user.fixture.UserTestFixture
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime

class NotificationRecipientPolicyTest :
    StringSpec({
        val settings = mockk<NotificationSettingRepository>()
        val preferences = mockk<NotificationPreferenceRepository>()
        val members = mockk<ClubMemberReader>()
        val clubs = mockk<ClubReader>()
        val accounts = mockk<AccountSettingReader>()
        val policy = NotificationRecipientPolicy(settings, preferences, members, clubs, accounts)
        val club = ClubTestFixture.createClub(1L)
        beforeTest {
            clearMocks(settings, preferences, members, clubs, accounts)
            every { settings.findAllByClubIdAndUserIdIn(any(), any()) } returns emptyList()
            every { preferences.findAllByClubIdAndCategoryAndUserIdIn(any(), any(), any()) } returns emptyList()
            every { members.findAllByClubIdAndUserIds(1L, any()) } answers {
                secondArg<List<Long>>().map {
                    ClubMemberTestFixture.createActiveMember(club = club, user = UserTestFixture.createActiveUser1(it))
                }
            }
            every { accounts.isVisibleToMembers(1L) } returns false
            every { clubs.findByIdOrNull(1L) } returns club
        }

        "미설정 대상은 기본 수신하고 ID 중복을 제거하며 설정은 한 번씩 일괄 조회한다" {
            policy.filterEligibleUserIds(1L, NotificationCategory.NOTICE, listOf(10L, 20L, 10L)) shouldBe
                listOf(10L, 20L)
            verify(exactly = 1) { settings.findAllByClubIdAndUserIdIn(1L, listOf(10L, 20L)) }
            verify(exactly = 1) {
                preferences.findAllByClubIdAndCategoryAndUserIdIn(1L, NotificationCategory.NOTICE, listOf(10L, 20L))
            }
        }

        "마스터 또는 해당 카테고리 OFF인 사용자를 제외한다" {
            every { settings.findAllByClubIdAndUserIdIn(any(), any()) } returns
                listOf(NotificationSetting.create(10L, 1L).apply { updateMaster(false) })
            every { preferences.findAllByClubIdAndCategoryAndUserIdIn(any(), any(), any()) } returns
                listOf(NotificationPreference.create(20L, 1L, NotificationCategory.NOTICE, false))
            policy.filterEligibleUserIds(1L, NotificationCategory.NOTICE, listOf(10L, 20L, 30L)) shouldBe listOf(30L)
        }

        "탈퇴하거나 비활성인 회원 및 동아리 비회원은 제외한다" {
            every { members.findAllByClubIdAndUserIds(1L, any()) } returns
                listOf(
                    ClubMemberTestFixture.createBannedMember(
                        club = club,
                        user = UserTestFixture.createActiveUser1(10L),
                    ),
                    ClubMemberTestFixture.createActiveMember(
                        club = club,
                        user =
                            UserTestFixture.createActiveUser2(20L).apply {
                                leave(LocalDateTime.now())
                            },
                    ),
                )
            policy.filterEligibleUserIds(1L, NotificationCategory.NOTICE, listOf(10L, 20L, 30L)) shouldBe emptyList()
            verify(exactly = 0) { settings.findAllByClubIdAndUserIdIn(any(), any()) }
        }

        "기능 미사용 경고·회비는 수신 설정과 무관하게 제외한다" {
            policy.filterEligibleUserIds(1L, NotificationCategory.DUES, listOf(10L)) shouldBe emptyList()
            policy.filterEligibleUserIds(1L, NotificationCategory.WARNING, listOf(10L)) shouldBe emptyList()
        }

        "후보가 없으면 DB 조회하지 않는다" {
            policy.filterEligibleUserIds(1L, NotificationCategory.NOTICE, emptyList()) shouldBe emptyList()
            verify(exactly = 0) { members.findAllByClubIdAndUserIds(any(), any()) }
        }
    })
