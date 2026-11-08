package com.weeth.domain.notification.application.usecase

import com.weeth.config.TestContainersConfig
import com.weeth.domain.account.domain.entity.AccountSetting
import com.weeth.domain.account.domain.repository.AccountSettingRepository
import com.weeth.domain.club.domain.repository.ClubMemberRepository
import com.weeth.domain.club.domain.repository.ClubRepository
import com.weeth.domain.club.fixture.ClubMemberTestFixture
import com.weeth.domain.notification.application.event.SendPushNotificationEventListener
import com.weeth.domain.notification.application.usecase.command.CreateNotificationUseCase
import com.weeth.domain.notification.application.usecase.command.ManageNotificationTokenUseCase
import com.weeth.domain.notification.domain.entity.NotificationPreference
import com.weeth.domain.notification.domain.entity.NotificationSetting
import com.weeth.domain.notification.domain.entity.NotificationToken
import com.weeth.domain.notification.domain.enums.NotificationCategory
import com.weeth.domain.notification.domain.enums.NotificationPlatform
import com.weeth.domain.notification.domain.enums.NotificationTemplateFailureReason
import com.weeth.domain.notification.domain.port.PushNotificationSenderPort
import com.weeth.domain.notification.domain.repository.NotificationFailureRepository
import com.weeth.domain.notification.domain.repository.NotificationPreferenceRepository
import com.weeth.domain.notification.domain.repository.NotificationSettingRepository
import com.weeth.domain.notification.domain.repository.NotificationTokenRepository
import com.weeth.domain.notification.domain.repository.UserNotificationRepository
import com.weeth.domain.notification.domain.service.NotificationRecipientPolicy
import com.weeth.domain.notification.domain.service.NotificationTemplateRenderer
import com.weeth.domain.notification.domain.vo.PushNotificationResult
import com.weeth.domain.notification.fixture.NotificationEventProbe
import com.weeth.domain.notification.fixture.NotificationPreferenceTestFixture
import com.weeth.domain.notification.fixture.NotificationRequestTestFixture
import com.weeth.domain.notification.infrastructure.CodeNotificationTemplateProvider
import com.weeth.domain.notification.infrastructure.JdbcUserNotificationBulkWriterAdapter
import com.weeth.domain.user.domain.repository.UserRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDateTime

@DataJpaTest
@Import(
    TestContainersConfig::class,
    CreateNotificationUseCase::class,
    NotificationRecipientPolicy::class,
    NotificationTemplateRenderer::class,
    CodeNotificationTemplateProvider::class,
    JdbcUserNotificationBulkWriterAdapter::class,
    NotificationEventProbe::class,
    ManageNotificationTokenUseCase::class,
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class NotificationGenerationIntegrationTest(
    private val create: CreateNotificationUseCase,
    private val clubs: ClubRepository,
    private val users: UserRepository,
    private val members: ClubMemberRepository,
    private val settings: NotificationSettingRepository,
    private val preferences: NotificationPreferenceRepository,
    private val accounts: AccountSettingRepository,
    private val notifications: UserNotificationRepository,
    private val failures: NotificationFailureRepository,
    private val tokens: NotificationTokenRepository,
    private val tokenUseCase: ManageNotificationTokenUseCase,
    private val recipients: NotificationRecipientPolicy,
    private val probe: NotificationEventProbe,
    private val transactionManager: PlatformTransactionManager,
) : StringSpec({
        beforeTest { probe.events.clear() }

        "마스터와 공지 설정을 생성 전에 필터링하고 미설정 사용자만 알림을 저장한다" {
            val club = clubs.save(NotificationPreferenceTestFixture.createClub())
            val targets = (1..3).map { users.save(NotificationPreferenceTestFixture.createUser()) }
            members.saveAll(targets.map { ClubMemberTestFixture.createActiveMember(club = club, user = it) })
            settings.save(NotificationSetting.create(targets[0].id, club.id).apply { updateMaster(false) })
            preferences.save(NotificationPreference.create(targets[1].id, club.id, NotificationCategory.NOTICE, false))
            create.execute(NotificationRequestTestFixture.createNotice(club.id), targets.map { it.id })
            notifications.findAll().filter { it.clubId == club.id }.let {
                it.map { notification -> notification.user.id } shouldBe listOf(targets[2].id)
                it.single().templateVersion shouldBe 1
            }
            probe.events.single().targetUserIds shouldBe listOf(targets[2].id)
            probe.events
                .single()
                .pushMessage.templateVersion shouldBe 1
        }

        "렌더링 실패는 원래 트랜잭션을 커밋하고 원문 대신 누락 변수와 채널을 기록한다" {
            val club = clubs.save(NotificationPreferenceTestFixture.createClub())
            val user = users.save(NotificationPreferenceTestFixture.createUser())
            members.save(ClubMemberTestFixture.createActiveMember(club = club, user = user))
            TransactionTemplate(transactionManager).executeWithoutResult {
                accounts.save(AccountSetting.createDefault(club.id))
                create.execute(NotificationRequestTestFixture.createNotice(club.id, title = null), listOf(user.id))
            }
            (accounts.findByClubId(club.id) != null) shouldBe true
            failures.findAll().single { it.clubId == club.id }.let {
                it.reason shouldBe NotificationTemplateFailureReason.MISSING_VARIABLES
                it.missingVariables shouldBe "noticeTitle"
                it.eventId shouldBe "NOTICE_CREATED:${club.id}:100"
            }
            notifications.findAll().none { it.clubId == club.id } shouldBe true
            probe.events.isEmpty() shouldBe true
        }

        "롤백된 생성은 알림과 커밋 이후 이벤트를 남기지 않으며 실패 이력도 함께 롤백한다" {
            val club = clubs.save(NotificationPreferenceTestFixture.createClub())
            val user = users.save(NotificationPreferenceTestFixture.createUser())
            members.save(ClubMemberTestFixture.createActiveMember(club = club, user = user))
            listOf("정상 공지", null).forEach { title ->
                shouldThrow<IllegalStateException> {
                    TransactionTemplate(transactionManager).executeWithoutResult {
                        create.execute(
                            NotificationRequestTestFixture.createNotice(club.id, title = title),
                            listOf(user.id),
                        )
                        probe.events.isEmpty() shouldBe true
                        error("rollback")
                    }
                }
            }
            notifications.findAll().none { it.clubId == club.id } shouldBe true
            failures.findAll().none { it.clubId == club.id } shouldBe true
            probe.events.isEmpty() shouldBe true
        }

        "생성 후 OFF와 탈퇴를 발송 직전에 반영하고 기존 알림함은 보존한다" {
            val club = clubs.save(NotificationPreferenceTestFixture.createClub())
            val targets = (1..3).map { users.save(NotificationPreferenceTestFixture.createUser()) }
            members.saveAll(targets.map { ClubMemberTestFixture.createActiveMember(club = club, user = it) })
            targets.forEach {
                tokens.save(
                    NotificationToken.create(it, "token-${it.id}", NotificationPlatform.WEB, LocalDateTime.now()),
                )
            }
            create.execute(NotificationRequestTestFixture.createNotice(club.id), targets.map { it.id })
            settings.save(NotificationSetting.create(targets[0].id, club.id).apply { updateMaster(false) })
            users.save(targets[1].apply { leave(LocalDateTime.now()) })
            val sender = mockk<PushNotificationSenderPort>()
            every { sender.sendMulticast(any()) } answers {
                TransactionSynchronizationManager.isActualTransactionActive() shouldBe false
                PushNotificationResult()
            }
            SendPushNotificationEventListener(tokens, sender, tokenUseCase, recipients).handle(probe.events.single())
            verify(exactly = 1) {
                sender.sendMulticast(
                    match {
                        it.targets.map { target -> target.token } ==
                            listOf("token-${targets[2].id}")
                    },
                )
            }
            notifications.findAll().count { it.clubId == club.id } shouldBe 3
        }
    })
