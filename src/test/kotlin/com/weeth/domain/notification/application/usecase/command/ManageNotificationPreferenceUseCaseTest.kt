package com.weeth.domain.notification.application.usecase.command

import com.weeth.domain.account.domain.repository.AccountSettingReader
import com.weeth.domain.club.application.exception.MemberNotActiveException
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.club.fixture.ClubMemberTestFixture
import com.weeth.domain.club.fixture.ClubTestFixture
import com.weeth.domain.notification.application.dto.request.NotificationPreferenceItemRequest
import com.weeth.domain.notification.application.dto.request.UpdateNotificationPreferencesRequest
import com.weeth.domain.notification.application.exception.InvalidNotificationPreferenceException
import com.weeth.domain.notification.application.exception.NotificationErrorCode
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
import org.springframework.dao.PessimisticLockingFailureException

class ManageNotificationPreferenceUseCaseTest :
    DescribeSpec({
        val members = mockk<ClubMemberPolicy>()
        val accounts = mockk<AccountSettingReader>()
        val settings = mockk<NotificationSettingRepository>()
        val preferences = mockk<NotificationPreferenceRepository>()
        val useCase =
            ManageNotificationPreferenceUseCase(
                members,
                accounts,
                settings,
                preferences,
                NotificationPreferencePolicy(),
            )
        val setting = NotificationSetting.create(10L, 1L)
        val notice = NotificationPreference.create(10L, 1L, NotificationCategory.NOTICE, false)

        beforeTest {
            clearMocks(members, accounts, settings, preferences)
            setting.updateMaster(true)
            notice.updateEnabled(false)
            every { members.getActiveMemberWithLock(1L, 10L) } returns
                ClubMemberTestFixture.createActiveMember(club = ClubTestFixture.createClub(1L))
            every { accounts.isVisibleToMembers(1L) } returns false
            every { settings.findByUserIdAndClubId(10L, 1L) } returns setting
            every { preferences.findAllByUserIdAndClubId(10L, 1L) } returns listOf(notice)
            every { settings.save(any()) } answers { firstArg() }
            every { preferences.saveAll(any<List<NotificationPreference>>()) } answers { firstArg() }
        }

        describe("설정 부분 변경") {
            it("마스터만 OFF해도 카테고리 선택값은 보존한다") {
                useCase.update(1L, 10L, UpdateNotificationPreferencesRequest(masterEnabled = false))
                setting.masterEnabled shouldBe false
                notice.enabled shouldBe false
                verify(exactly = 0) { preferences.saveAll(any<List<NotificationPreference>>()) }
            }

            it("마스터 ON과 개별 변경을 함께 허용하고 목록 밖 선택은 유지한다") {
                setting.updateMaster(false)
                useCase.update(
                    1L,
                    10L,
                    UpdateNotificationPreferencesRequest(
                        true,
                        listOf(NotificationPreferenceItemRequest(NotificationCategory.SCHEDULE, false)),
                    ),
                )
                setting.masterEnabled shouldBe true
                notice.enabled shouldBe false
                verify {
                    preferences.saveAll(
                        match<List<NotificationPreference>> {
                            it.single().category ==
                                NotificationCategory.SCHEDULE &&
                                !it.single().enabled
                        },
                    )
                }
            }

            it("빈 PATCH는 쓰기를 수행하지 않는다") {
                useCase.update(1L, 10L, UpdateNotificationPreferencesRequest())
                verify(exactly = 0) {
                    settings.save(any())
                    preferences.saveAll(any<List<NotificationPreference>>())
                }
            }

            it("기능 사용 불가 항목이 섞이면 마스터와 기존 선택을 변경하지 않는다") {
                val exception =
                    shouldThrow<InvalidNotificationPreferenceException> {
                        useCase.update(
                            1L,
                            10L,
                            UpdateNotificationPreferencesRequest(
                                true,
                                listOf(
                                    NotificationPreferenceItemRequest(NotificationCategory.NOTICE, true),
                                    NotificationPreferenceItemRequest(NotificationCategory.DUES, false),
                                ),
                            ),
                        )
                    }
                exception.errorCode shouldBe NotificationErrorCode.NOTIFICATION_CATEGORY_UNAVAILABLE
                setting.masterEnabled shouldBe true
                notice.enabled shouldBe false
                verify(exactly = 0) {
                    settings.save(any())
                    preferences.saveAll(any<List<NotificationPreference>>())
                }
            }

            it("마스터 OFF 상태의 개별 변경은 거부한다") {
                setting.updateMaster(false)
                shouldThrow<InvalidNotificationPreferenceException> {
                    useCase.update(
                        1L,
                        10L,
                        UpdateNotificationPreferencesRequest(
                            preferences = listOf(NotificationPreferenceItemRequest(NotificationCategory.NOTICE, true)),
                        ),
                    )
                }.errorCode shouldBe NotificationErrorCode.NOTIFICATION_MASTER_DISABLED
                notice.enabled shouldBe false
            }

            it("중복 카테고리는 변경 전에 거부한다") {
                shouldThrow<InvalidNotificationPreferenceException> {
                    useCase.update(
                        1L,
                        10L,
                        UpdateNotificationPreferencesRequest(
                            preferences =
                                listOf(
                                    NotificationPreferenceItemRequest(NotificationCategory.NOTICE, true),
                                    NotificationPreferenceItemRequest(NotificationCategory.NOTICE, false),
                                ),
                        ),
                    )
                }.errorCode shouldBe NotificationErrorCode.NOTIFICATION_DUPLICATE_CATEGORY
                notice.enabled shouldBe false
            }

            it("비활성 회원은 저장소에 접근하기 전에 거부한다") {
                every { members.getActiveMemberWithLock(1L, 10L) } throws MemberNotActiveException()
                shouldThrow<MemberNotActiveException> {
                    useCase.update(
                        1L,
                        10L,
                        UpdateNotificationPreferencesRequest(false),
                    )
                }
                verify(exactly = 0) {
                    settings.findByUserIdAndClubId(any(), any())
                    settings.save(any())
                }
            }

            it("동시 갱신 잠금 실패는 도메인 오류로 변환한다") {
                every { members.getActiveMemberWithLock(1L, 10L) } throws PessimisticLockingFailureException("timeout")
                shouldThrow<InvalidNotificationPreferenceException> {
                    useCase.update(1L, 10L, UpdateNotificationPreferencesRequest(false))
                }.errorCode shouldBe NotificationErrorCode.NOTIFICATION_PREFERENCE_LOCK_TIMEOUT
            }
        }
    })
