package com.weeth.domain.notification.application.usecase

import com.ninjasquad.springmockk.MockkBean
import com.weeth.config.TestContainersConfig
import com.weeth.domain.account.application.dto.request.SaveAccountBankAccountRequest
import com.weeth.domain.account.application.dto.request.SaveAccountBasicRequest
import com.weeth.domain.account.application.dto.request.SaveAccountCarryOverRequest
import com.weeth.domain.account.application.dto.request.SavePaymentTargetsRequest
import com.weeth.domain.account.application.event.DuesObligationCreatedEvent
import com.weeth.domain.account.application.usecase.command.RegisterAccountUseCase
import com.weeth.domain.account.domain.entity.AccountSetting
import com.weeth.domain.account.domain.enums.AccountStatus
import com.weeth.domain.account.domain.repository.AccountPaymentTargetRepository
import com.weeth.domain.account.domain.repository.AccountRepository
import com.weeth.domain.account.domain.repository.AccountSettingRepository
import com.weeth.domain.account.domain.vo.Money
import com.weeth.domain.cardinal.domain.repository.CardinalRepository
import com.weeth.domain.cardinal.fixture.CardinalTestFixture
import com.weeth.domain.club.domain.entity.Club
import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.club.domain.enums.MemberRole
import com.weeth.domain.club.domain.repository.ClubMemberCardinalRepository
import com.weeth.domain.club.domain.repository.ClubMemberRepository
import com.weeth.domain.club.domain.repository.ClubRepository
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.club.domain.service.ClubPermissionPolicy
import com.weeth.domain.club.fixture.ClubMemberCardinalTestFixture
import com.weeth.domain.notification.application.event.CreateDuesNotificationEventListener
import com.weeth.domain.notification.application.event.NotificationCreatedEvent
import com.weeth.domain.notification.application.event.SendPushNotificationEventListener
import com.weeth.domain.notification.application.usecase.command.CreateNotificationUseCase
import com.weeth.domain.notification.application.usecase.command.EnqueueNotificationDeliveryUseCase
import com.weeth.domain.notification.application.usecase.command.ManageNotificationDeliveryUseCase
import com.weeth.domain.notification.application.usecase.command.ManageNotificationTokenUseCase
import com.weeth.domain.notification.domain.entity.NotificationPreference
import com.weeth.domain.notification.domain.entity.NotificationSetting
import com.weeth.domain.notification.domain.entity.NotificationToken
import com.weeth.domain.notification.domain.enums.NotificationCategory
import com.weeth.domain.notification.domain.enums.NotificationDeliveryAction
import com.weeth.domain.notification.domain.enums.NotificationPlatform
import com.weeth.domain.notification.domain.enums.NotificationReferenceType
import com.weeth.domain.notification.domain.enums.NotificationType
import com.weeth.domain.notification.domain.enums.PushTargetStatus
import com.weeth.domain.notification.domain.port.PushNotificationSenderPort
import com.weeth.domain.notification.domain.port.UserNotificationBulkWriter
import com.weeth.domain.notification.domain.repository.NotificationDeliveryRepository
import com.weeth.domain.notification.domain.repository.NotificationEventRepository
import com.weeth.domain.notification.domain.repository.NotificationFailureRepository
import com.weeth.domain.notification.domain.repository.NotificationPreferenceRepository
import com.weeth.domain.notification.domain.repository.NotificationSettingRepository
import com.weeth.domain.notification.domain.repository.NotificationTokenRepository
import com.weeth.domain.notification.domain.repository.UserNotificationRepository
import com.weeth.domain.notification.domain.service.DuesNotificationPolicy
import com.weeth.domain.notification.domain.service.NotificationRecipientPolicy
import com.weeth.domain.notification.domain.service.NotificationRetryPolicy
import com.weeth.domain.notification.domain.service.NotificationTemplateRenderer
import com.weeth.domain.notification.domain.service.SessionDayReminderPolicy
import com.weeth.domain.notification.domain.vo.NotificationContent
import com.weeth.domain.notification.domain.vo.NotificationQueueMessage
import com.weeth.domain.notification.domain.vo.PushNotificationResult
import com.weeth.domain.notification.domain.vo.PushTargetOutcome
import com.weeth.domain.notification.domain.vo.RenderedNotification
import com.weeth.domain.notification.fixture.NotificationIntegrationTestFixture.saveMember
import com.weeth.domain.notification.fixture.NotificationPreferenceTestFixture
import com.weeth.domain.notification.infrastructure.CodeNotificationTemplateProvider
import com.weeth.domain.notification.infrastructure.JdbcUserNotificationBulkWriterAdapter
import com.weeth.domain.user.domain.repository.UserRepository
import com.weeth.global.common.id.TsidBase62Encoder
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Import
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@DataJpaTest(properties = ["notification.dispatch.enabled=true"])
@Import(
    TestContainersConfig::class,
    RegisterAccountUseCase::class,
    ClubPermissionPolicy::class,
    ClubMemberPolicy::class,
    CreateDuesNotificationEventListener::class,
    DuesNotificationPolicy::class,
    CreateNotificationUseCase::class,
    EnqueueNotificationDeliveryUseCase::class,
    ManageNotificationDeliveryUseCase::class,
    ManageNotificationTokenUseCase::class,
    NotificationRetryPolicy::class,
    SessionDayReminderPolicy::class,
    NotificationRecipientPolicy::class,
    NotificationTemplateRenderer::class,
    CodeNotificationTemplateProvider::class,
    JdbcUserNotificationBulkWriterAdapter::class,
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class DuesNotificationIntegrationTest(
    private val registration: RegisterAccountUseCase,
    private val management: ManageNotificationDeliveryUseCase,
    private val accounts: AccountRepository,
    private val targets: AccountPaymentTargetRepository,
    private val accountSettings: AccountSettingRepository,
    private val clubs: ClubRepository,
    private val users: UserRepository,
    private val members: ClubMemberRepository,
    private val cardinals: CardinalRepository,
    private val memberCardinals: ClubMemberCardinalRepository,
    private val preferences: NotificationPreferenceRepository,
    private val settings: NotificationSettingRepository,
    private val notifications: UserNotificationRepository,
    private val deliveries: NotificationDeliveryRepository,
    private val tokens: NotificationTokenRepository,
    private val recipients: NotificationRecipientPolicy,
    private val duesPolicy: DuesNotificationPolicy,
    private val tokenUseCase: ManageNotificationTokenUseCase,
    private val publisher: ApplicationEventPublisher,
    private val bulkWriter: UserNotificationBulkWriter,
    private val renderer: NotificationTemplateRenderer,
    private val failures: NotificationFailureRepository,
    private val events: NotificationEventRepository,
    private val enqueue: EnqueueNotificationDeliveryUseCase,
    private val transactionManager: PlatformTransactionManager,
    @MockkBean private val clock: Clock,
) : StringSpec({
        val transaction = TransactionTemplate(transactionManager)
        val base = Instant.parse("2026-11-10T03:00:00Z")

        fun advance(seconds: Long) {
            every { clock.zone } returns ZoneId.of("Asia/Seoul")
            every { clock.instant() } returns base.plusSeconds(seconds)
        }
        beforeTest { advance(0) }

        data class Fixture(
            val club: Club,
            val accountId: Long,
            val members: Map<String, ClubMember>,
        ) {
            val adminId: Long get() =
                this.members
                    .getValue("admin")
                    .user.id
        }

        fun ready(visible: Boolean = true): Fixture {
            val club = clubs.save(NotificationPreferenceTestFixture.createClub())
            val cardinal = cardinals.save(CardinalTestFixture.createCardinal(club = club, cardinalNumber = 1))
            val roster =
                listOf("admin", "normal", "paid", "excluded", "category-off", "master-off", "banned", "inactive")
                    .associateWith { kind ->
                        val member =
                            members.saveMember(
                                club,
                                users,
                                role = if (kind == "admin") MemberRole.ADMIN else MemberRole.USER,
                            )
                        memberCardinals.save(ClubMemberCardinalTestFixture.create(member, cardinal))
                        member
                    }
            accountSettings.save(AccountSetting.createDefault(club.id).apply { if (visible) showToMembers() })
            preferences.save(
                NotificationPreference.create(
                    roster.getValue("category-off").user.id,
                    club.id,
                    NotificationCategory.DUES,
                    false,
                ),
            )
            settings.save(
                NotificationSetting
                    .create(
                        roster.getValue("master-off").user.id,
                        club.id,
                    ).apply { updateMaster(false) },
            )
            val adminId = roster.getValue("admin").user.id
            val accountId = registration.createDraft(club.id, 1, adminId).accountId
            registration.saveBasic(club.id, accountId, SaveAccountBasicRequest("정기 회비", 30_000, "운영비"), adminId)
            registration.savePaymentTargets(
                club.id,
                accountId,
                SavePaymentTargetsRequest(
                    roster.filterKeys { it != "excluded" }.values.map { it.id },
                ),
                adminId,
            )
            registration.saveCarryOver(club.id, accountId, SaveAccountCarryOverRequest(false, null, null), adminId)
            registration.saveBankAccount(club.id, accountId, SaveAccountBankAccountRequest(false, null), adminId)
            transaction.executeWithoutResult {
                val paid = requireNotNull(targets.findByAccountIdAndClubMemberId(accountId, roster.getValue("paid").id))
                paid.markPaid(Money.of(30_000), adminId, LocalDateTime.now(clock))
                targets.save(paid)
                members.save(roster.getValue("banned").apply { ban() })
                users.save(roster.getValue("inactive").user.apply { ban() })
            }
            return Fixture(club, accountId, roster)
        }

        fun complete(f: Fixture) = registration.completeRegistration(f.club.id, f.accountId, f.adminId)

        fun event(f: Fixture): DuesObligationCreatedEvent {
            val target =
                requireNotNull(targets.findByAccountIdAndClubMemberId(f.accountId, f.members.getValue("normal").id))
            return DuesObligationCreatedEvent(
                f.club.id,
                f.accountId,
                target.id,
                f.members
                    .getValue("normal")
                    .user.id,
                "정기 회비",
                30_000,
            )
        }

        "DRAFT는 알림 없이 저장하고 활성화 시 본인 의무·수신 설정을 충족한 회원에게만 알린다" {
            val f = ready()
            notifications.findAll().none { it.clubId == f.club.id } shouldBe true
            complete(f)
            val saved = notifications.findAll().filter { it.clubId == f.club.id }
            saved.map { it.user.id }.toSet() shouldBe
                setOf(
                    f.adminId,
                    f.members
                        .getValue("normal")
                        .user.id,
                )
            saved.all { it.type == NotificationType.DUES_OBLIGATION_CREATED } shouldBe true
            saved.first().body shouldContain "정기 회비"
            saved.first().body shouldContain "30,000"
            saved.first().targetPath shouldBe "/clubs/${TsidBase62Encoder.encode(f.club.id)}/accounts"
            deliveries.findAll().count { it.clubId == f.club.id } shouldBe 2
            transaction.executeWithoutResult { publisher.publishEvent(event(f)) }
            notifications.findAll().count { it.clubId == f.club.id } shouldBe 2
        }

        "기능 비공개 시 차단하며 공개 후 동일 이벤트를 다시 전달해도 소급 발송하지 않는다" {
            val f = ready(visible = false)
            complete(f)
            notifications.findAll().none { it.clubId == f.club.id } shouldBe true
            accountSettings.save(requireNotNull(accountSettings.findByClubId(f.club.id)).apply { showToMembers() })
            transaction.executeWithoutResult { publisher.publishEvent(event(f)) }
            notifications.findAll().none { it.clubId == f.club.id } shouldBe true
        }

        "legacy 모드에서도 동일 회비 이벤트는 DB 알림과 푸시 이벤트를 한 번만 생성한다" {
            val f = ready()
            transaction.executeWithoutResult {
                accounts.findById(f.accountId).orElseThrow().activate()
            }
            val legacyPublisher = mockk<ApplicationEventPublisher>(relaxed = true)
            val listener =
                CreateDuesNotificationEventListener(
                    CreateNotificationUseCase(
                        users,
                        bulkWriter,
                        legacyPublisher,
                        recipients,
                        renderer,
                        failures,
                        enqueue,
                        events,
                        durableEnabled = false,
                    ),
                )
            repeat(2) {
                transaction.executeWithoutResult { listener.handle(event(f)) }
            }
            val saved = notifications.findAll().filter { it.clubId == f.club.id }
            saved.size shouldBe 1
            saved.single().eventId shouldBe "DUES_OBLIGATION_CREATED:${f.accountId}:${event(f).paymentTargetId}"
            verify(exactly = 1) { legacyPublisher.publishEvent(any<NotificationCreatedEvent>()) }
            deliveries.findAll().none { it.clubId == f.club.id } shouldBe true
        }

        "활성화 트랜잭션 롤백은 장부 상태와 알림 작업을 함께 되돌린다" {
            val f = ready()
            shouldThrow<IllegalStateException> {
                transaction.executeWithoutResult {
                    complete(f)
                    error("rollback")
                }
            }
            accounts.findById(f.accountId).orElseThrow().status shouldBe AccountStatus.DRAFT
            notifications.findAll().none { it.clubId == f.club.id } shouldBe true
            deliveries.findAll().none { it.clubId == f.club.id } shouldBe true
        }

        "발송 전 납부·제외된 대상은 durable·legacy 경로 모두 전송하지 않고 알림함은 보존한다" {
            val f = ready()
            complete(f)
            val normal = f.members.getValue("normal").user
            tokens.save(
                NotificationToken.create(
                    normal,
                    "dues-${normal.id}",
                    NotificationPlatform.WEB,
                    LocalDateTime.now(clock),
                ),
            )
            val delivery = deliveries.findAll().single { it.clubId == f.club.id && it.userId == normal.id }
            val message = NotificationQueueMessage(delivery.id, delivery.eventId)
            val attempt = requireNotNull(management.prepare(message).attempt)
            attempt.command.body shouldContain "30,000"
            management.complete(
                attempt,
                PushNotificationResult(
                    outcomes =
                        listOf(
                            PushTargetOutcome(
                                attempt.targets
                                    .single()
                                    .target.token,
                                PushTargetStatus.RETRYABLE,
                                "UNAVAILABLE",
                            ),
                        ),
                ),
            )
            transaction.executeWithoutResult {
                val target =
                    requireNotNull(targets.findByAccountIdAndClubMemberId(f.accountId, f.members.getValue("normal").id))
                target.markPaid(Money.of(30_000), f.adminId, LocalDateTime.now(clock))
                targets.save(target)
                val admin =
                    requireNotNull(targets.findByAccountIdAndClubMemberId(f.accountId, f.members.getValue("admin").id))
                admin.exclude()
                targets.save(admin)
            }
            advance(20)
            management.prepare(message).action shouldBe NotificationDeliveryAction.ACK
            deliveries.findById(delivery.id).orElseThrow().lastErrorCode shouldBe "DUES_OBLIGATION_NO_LONGER_VALID"
            val adminDelivery = deliveries.findAll().single { it.clubId == f.club.id && it.userId == f.adminId }
            management.prepare(NotificationQueueMessage(adminDelivery.id, adminDelivery.eventId)).action shouldBe
                NotificationDeliveryAction.ACK
            val sender = mockk<PushNotificationSenderPort>(relaxed = true)
            val content =
                NotificationContent.create(
                    delivery.type,
                    delivery.title,
                    delivery.body,
                    "/clubs/${TsidBase62Encoder.encode(f.club.id)}/accounts",
                    f.club.id,
                    NotificationReferenceType.ACCOUNT_PAYMENT_TARGET,
                    event(f).paymentTargetId,
                    delivery.data,
                )
            SendPushNotificationEventListener(tokens, sender, tokenUseCase, recipients, duesPolicy).handle(
                NotificationCreatedEvent(
                    content,
                    listOf(normal.id),
                    RenderedNotification(delivery.title, delivery.body, 1),
                    delivery.eventId,
                ),
            )
            verify(exactly = 0) { sender.sendMulticast(any()) }
            notifications.findAll().count { it.clubId == f.club.id } shouldBe 2
        }
    })
