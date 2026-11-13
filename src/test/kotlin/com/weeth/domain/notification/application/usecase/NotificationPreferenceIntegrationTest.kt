package com.weeth.domain.notification.application.usecase

import com.weeth.config.TestContainersConfig
import com.weeth.domain.account.domain.entity.AccountSetting
import com.weeth.domain.account.domain.repository.AccountSettingRepository
import com.weeth.domain.club.application.exception.ClubMemberNotFoundException
import com.weeth.domain.club.domain.repository.ClubMemberRepository
import com.weeth.domain.club.domain.repository.ClubRepository
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.club.fixture.ClubMemberTestFixture
import com.weeth.domain.notification.application.dto.request.NotificationPreferenceItemRequest
import com.weeth.domain.notification.application.dto.request.UpdateNotificationPreferencesRequest
import com.weeth.domain.notification.application.exception.InvalidNotificationPreferenceException
import com.weeth.domain.notification.application.mapper.NotificationPreferenceMapper
import com.weeth.domain.notification.application.usecase.command.ManageNotificationPreferenceUseCase
import com.weeth.domain.notification.application.usecase.query.GetNotificationPreferenceQueryService
import com.weeth.domain.notification.domain.enums.NotificationCategory
import com.weeth.domain.notification.domain.repository.NotificationPreferenceRepository
import com.weeth.domain.notification.domain.repository.NotificationSettingRepository
import com.weeth.domain.notification.domain.service.NotificationPreferencePolicy
import com.weeth.domain.notification.fixture.NotificationPreferenceTestFixture
import com.weeth.domain.user.domain.repository.UserRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@DataJpaTest
@Import(
    TestContainersConfig::class,
    ClubMemberPolicy::class,
    NotificationPreferencePolicy::class,
    NotificationPreferenceMapper::class,
    ManageNotificationPreferenceUseCase::class,
    GetNotificationPreferenceQueryService::class,
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class NotificationPreferenceIntegrationTest(
    private val useCase: ManageNotificationPreferenceUseCase,
    private val query: GetNotificationPreferenceQueryService,
    private val clubs: ClubRepository,
    private val users: UserRepository,
    private val members: ClubMemberRepository,
    private val accounts: AccountSettingRepository,
    private val settings: NotificationSettingRepository,
    private val preferences: NotificationPreferenceRepository,
    private val transactionManager: PlatformTransactionManager,
) : StringSpec({
        "조회는 행을 생성하지 않고 동아리와 사용자를 격리한다" {
            val club = clubs.save(NotificationPreferenceTestFixture.createClub())
            val otherClub = clubs.save(NotificationPreferenceTestFixture.createClub())
            val user = users.save(NotificationPreferenceTestFixture.createUser())
            val otherUser = users.save(NotificationPreferenceTestFixture.createUser())
            members.saveAll(
                listOf(
                    ClubMemberTestFixture.createActiveMember(club = club, user = user),
                    ClubMemberTestFixture.createActiveMember(club = club, user = otherUser),
                    ClubMemberTestFixture.createActiveMember(club = otherClub, user = user),
                ),
            )
            query.find(club.id, user.id).masterEnabled shouldBe true
            settings.findByUserIdAndClubId(user.id, club.id) shouldBe null
            preferences.findAllByUserIdAndClubId(user.id, club.id) shouldBe emptyList()
            useCase.update(club.id, user.id, UpdateNotificationPreferencesRequest(false))
            query.find(club.id, user.id).masterEnabled shouldBe false
            query.find(otherClub.id, user.id).masterEnabled shouldBe true
            query.find(club.id, otherUser.id).masterEnabled shouldBe true
            shouldThrow<ClubMemberNotFoundException> { query.find(otherClub.id, otherUser.id) }
            shouldThrow<ClubMemberNotFoundException> {
                useCase.update(otherClub.id, otherUser.id, UpdateNotificationPreferencesRequest(false))
            }
        }

        "마스터 토글과 기능 비공개는 저장한 선택값을 지우지 않는다" {
            val club = clubs.save(NotificationPreferenceTestFixture.createClub())
            val user = users.save(NotificationPreferenceTestFixture.createUser())
            members.save(ClubMemberTestFixture.createActiveMember(club = club, user = user))
            val account = accounts.save(AccountSetting.createDefault(club.id).apply { showToMembers() })
            useCase.update(
                club.id,
                user.id,
                UpdateNotificationPreferencesRequest(
                    preferences = listOf(NotificationPreferenceItemRequest(NotificationCategory.DUES, false)),
                ),
            )
            useCase.update(club.id, user.id, UpdateNotificationPreferencesRequest(false))
            query.find(club.id, user.id).preferences.all { !it.effectiveEnabled } shouldBe true
            useCase.update(club.id, user.id, UpdateNotificationPreferencesRequest(true))
            accounts.save(account.apply { hideFromMembers() })
            query.find(club.id, user.id).preferences.first { it.category == NotificationCategory.DUES }.let {
                it.available shouldBe false
                it.enabled shouldBe false
            }
            accounts.save(account.apply { showToMembers() })
            query.find(club.id, user.id).preferences.first { it.category == NotificationCategory.DUES }.let {
                it.available shouldBe true
                it.enabled shouldBe false
                it.effectiveEnabled shouldBe false
            }
        }

        "일괄 검증 실패와 상위 트랜잭션 롤백은 부분 저장을 남기지 않는다" {
            val club = clubs.save(NotificationPreferenceTestFixture.createClub())
            val user = users.save(NotificationPreferenceTestFixture.createUser())
            members.save(ClubMemberTestFixture.createActiveMember(club = club, user = user))
            shouldThrow<InvalidNotificationPreferenceException> {
                useCase.update(
                    club.id,
                    user.id,
                    UpdateNotificationPreferencesRequest(
                        preferences =
                            listOf(
                                NotificationPreferenceItemRequest(NotificationCategory.NOTICE, false),
                                NotificationPreferenceItemRequest(NotificationCategory.WARNING, false),
                            ),
                    ),
                )
            }
            preferences.findAllByUserIdAndClubId(user.id, club.id) shouldBe emptyList()
            shouldThrow<IllegalStateException> {
                TransactionTemplate(transactionManager).executeWithoutResult {
                    useCase.update(
                        club.id,
                        user.id,
                        UpdateNotificationPreferencesRequest(
                            preferences = listOf(NotificationPreferenceItemRequest(NotificationCategory.NOTICE, false)),
                        ),
                    )
                    error("rollback")
                }
            }
            preferences.findAllByUserIdAndClubId(user.id, club.id) shouldBe emptyList()
            settings.findByUserIdAndClubId(user.id, club.id) shouldBe null
        }

        "동시 최초 PATCH가 서로 다른 카테고리를 갱신해도 중복 생성과 선택값 유실이 없다" {
            val club = clubs.save(NotificationPreferenceTestFixture.createClub())
            val user = users.save(NotificationPreferenceTestFixture.createUser())
            members.save(ClubMemberTestFixture.createActiveMember(club = club, user = user))
            val executor = Executors.newFixedThreadPool(2)
            val ready = CountDownLatch(2)
            val start = CountDownLatch(1)
            try {
                val futures =
                    listOf(NotificationCategory.NOTICE, NotificationCategory.SCHEDULE).map { category ->
                        executor.submit {
                            ready.countDown()
                            check(start.await(10, TimeUnit.SECONDS))
                            useCase.update(
                                club.id,
                                user.id,
                                UpdateNotificationPreferencesRequest(
                                    preferences = listOf(NotificationPreferenceItemRequest(category, false)),
                                ),
                            )
                        }
                    }
                check(ready.await(10, TimeUnit.SECONDS))
                start.countDown()
                futures.forEach { it.get(15, TimeUnit.SECONDS) }
                preferences.findAllByUserIdAndClubId(user.id, club.id).size shouldBe 2
                query
                    .find(club.id, user.id)
                    .preferences
                    .filter {
                        it.category in
                            setOf(NotificationCategory.NOTICE, NotificationCategory.SCHEDULE)
                    }.all { !it.enabled } shouldBe
                    true
                settings.findAll().count { it.userId == user.id && it.clubId == club.id } shouldBe 1
            } finally {
                executor.shutdownNow()
            }
        }
    })
