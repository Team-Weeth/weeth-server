package com.weeth.domain.notification.application.usecase

import com.ninjasquad.springmockk.MockkBean
import com.weeth.config.TestContainersConfig
import com.weeth.domain.cardinal.domain.entity.Cardinal
import com.weeth.domain.cardinal.domain.repository.CardinalRepository
import com.weeth.domain.cardinal.fixture.CardinalTestFixture
import com.weeth.domain.club.domain.entity.Club
import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.club.domain.enums.MemberRole
import com.weeth.domain.club.domain.repository.ClubMemberCardinalRepository
import com.weeth.domain.club.domain.repository.ClubMemberRepository
import com.weeth.domain.club.domain.repository.ClubRepository
import com.weeth.domain.club.domain.service.ClubMemberCardinalPolicy
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.club.domain.service.ClubPermissionPolicy
import com.weeth.domain.file.domain.port.FileAccessUrlPort
import com.weeth.domain.notification.application.event.CreateWarningNotificationEventListener
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
import com.weeth.domain.notification.domain.repository.NotificationOutboxRepository
import com.weeth.domain.notification.domain.repository.NotificationPreferenceRepository
import com.weeth.domain.notification.domain.repository.NotificationSettingRepository
import com.weeth.domain.notification.domain.repository.NotificationTokenRepository
import com.weeth.domain.notification.domain.repository.UserNotificationRepository
import com.weeth.domain.notification.domain.service.DuesNotificationPolicy
import com.weeth.domain.notification.domain.service.NotificationRecipientPolicy
import com.weeth.domain.notification.domain.service.NotificationRetryPolicy
import com.weeth.domain.notification.domain.service.NotificationTemplateRenderer
import com.weeth.domain.notification.domain.service.SessionDayReminderPolicy
import com.weeth.domain.notification.domain.service.WarningNotificationPolicy
import com.weeth.domain.notification.domain.vo.NotificationQueueMessage
import com.weeth.domain.notification.domain.vo.PushNotificationResult
import com.weeth.domain.notification.domain.vo.PushTargetOutcome
import com.weeth.domain.notification.fixture.NotificationIntegrationTestFixture.saveMemberWithCardinals
import com.weeth.domain.notification.fixture.NotificationPreferenceTestFixture
import com.weeth.domain.notification.infrastructure.CodeNotificationTemplateProvider
import com.weeth.domain.notification.infrastructure.JdbcUserNotificationBulkWriterAdapter
import com.weeth.domain.penalty.application.dto.request.SavePenaltyRequest
import com.weeth.domain.penalty.application.dto.request.UpdatePenaltyRequest
import com.weeth.domain.penalty.application.event.WarningAssignedEvent
import com.weeth.domain.penalty.application.mapper.PenaltyMapper
import com.weeth.domain.penalty.application.usecase.command.DeletePenaltyUseCase
import com.weeth.domain.penalty.application.usecase.command.SavePenaltyUseCase
import com.weeth.domain.penalty.application.usecase.command.UpdatePenaltyUseCase
import com.weeth.domain.penalty.domain.entity.Penalty
import com.weeth.domain.penalty.domain.enums.PenaltyType
import com.weeth.domain.penalty.domain.repository.PenaltyRepository
import com.weeth.domain.user.domain.repository.UserRepository
import com.weeth.global.common.id.TsidBase62Encoder
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Import
import org.springframework.test.util.ReflectionTestUtils
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
    SavePenaltyUseCase::class,
    UpdatePenaltyUseCase::class,
    DeletePenaltyUseCase::class,
    PenaltyMapper::class,
    ClubPermissionPolicy::class,
    ClubMemberPolicy::class,
    ClubMemberCardinalPolicy::class,
    CreateWarningNotificationEventListener::class,
    WarningNotificationPolicy::class,
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
class WarningNotificationIntegrationTest(
    private val save: SavePenaltyUseCase,
    private val update: UpdatePenaltyUseCase,
    private val delete: DeletePenaltyUseCase,
    private val management: ManageNotificationDeliveryUseCase,
    private val penalties: PenaltyRepository,
    private val clubs: ClubRepository,
    private val users: UserRepository,
    private val members: ClubMemberRepository,
    private val cardinals: CardinalRepository,
    private val memberCardinals: ClubMemberCardinalRepository,
    private val preferences: NotificationPreferenceRepository,
    private val settings: NotificationSettingRepository,
    private val notifications: UserNotificationRepository,
    private val deliveries: NotificationDeliveryRepository,
    private val outboxes: NotificationOutboxRepository,
    private val tokens: NotificationTokenRepository,
    private val recipients: NotificationRecipientPolicy,
    private val duesPolicy: DuesNotificationPolicy,
    private val warningPolicy: WarningNotificationPolicy,
    private val tokenUseCase: ManageNotificationTokenUseCase,
    private val publisher: ApplicationEventPublisher,
    private val bulkWriter: UserNotificationBulkWriter,
    private val renderer: NotificationTemplateRenderer,
    private val failures: NotificationFailureRepository,
    private val events: NotificationEventRepository,
    private val enqueue: EnqueueNotificationDeliveryUseCase,
    private val transactionManager: PlatformTransactionManager,
    @MockkBean private val clock: Clock,
    @MockkBean private val fileAccess: FileAccessUrlPort,
) : StringSpec({
        val transaction = TransactionTemplate(transactionManager)
        val base = Instant.parse("2026-11-23T03:00:00Z")
        val reason = "정기모임 무단 불참"
        beforeTest {
            every { clock.zone } returns ZoneId.of("Asia/Seoul")
            every { clock.instant() } returns base
        }

        data class Fixture(
            val club: Club,
            val cardinal: Cardinal,
            val roster: Map<String, ClubMember>,
        ) {
            val adminId: Long get() = roster.getValue("admin").user.id
        }

        fun ready(): Fixture {
            val club =
                clubs.save(
                    NotificationPreferenceTestFixture.createClub().apply {
                        ReflectionTestUtils.setField(this, "warningEnabled", true)
                    },
                )
            val cardinal = cardinals.save(CardinalTestFixture.createCardinal(club = club, cardinalNumber = 1))
            val roster =
                listOf("admin", "normal", "other", "category-off", "master-off").associateWith { kind ->
                    members.saveMemberWithCardinals(
                        club,
                        users,
                        memberCardinals,
                        cardinal,
                        role = if (kind == "admin") MemberRole.ADMIN else MemberRole.USER,
                    )
                }
            preferences.save(
                NotificationPreference.create(
                    roster.getValue("category-off").user.id,
                    club.id,
                    NotificationCategory.WARNING,
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
            return Fixture(club, cardinal, roster)
        }

        fun assign(
            f: Fixture,
            names: List<String> = listOf("normal"),
            type: PenaltyType = PenaltyType.WARNING,
        ) = save.save(
            f.club.id,
            f.adminId,
            SavePenaltyRequest(
                names.map {
                    f.roster
                        .getValue(it)
                        .user.id
                },
                reason,
                type,
            ),
        )

        fun rows(f: Fixture): List<Penalty> =
            checkNotNull(
                transaction.execute {
                    penalties.findAll().filter { it.clubMember.club.id == f.club.id }
                },
            )

        fun event(
            f: Fixture,
            penalty: Penalty,
        ) = WarningAssignedEvent(f.club.id, penalty.id, penalty.clubMember.user.id, reason)

        fun registerToken(member: ClubMember) =
            tokens.save(
                NotificationToken.create(
                    member.user,
                    "warning-${member.user.id}",
                    NotificationPlatform.WEB,
                    LocalDateTime.now(clock),
                ),
            )

        "대상 본인과 수신 설정을 적용하고 앱에는 사유를, PUSH에는 일반 문구만 저장하며 중복을 막는다" {
            val f = ready()
            assign(f, listOf("normal", "category-off", "master-off"))
            val original = rows(f).single { it.clubMember.id == f.roster.getValue("normal").id }
            val app = notifications.findAll().single { it.clubId == f.club.id }
            app.user.id shouldBe
                f.roster
                    .getValue("normal")
                    .user.id
            app.type shouldBe NotificationType.WARNING_ASSIGNED
            app.referenceType shouldBe NotificationReferenceType.PENALTY
            app.referenceId shouldBe original.id
            app.eventId shouldBe "WARNING_ASSIGNED:${f.club.id}:${original.id}"
            app.body shouldContain reason
            app.targetPath shouldBe "/clubs/${TsidBase62Encoder.encode(f.club.id)}/penalties"
            val delivery = deliveries.findAll().single { it.clubId == f.club.id }
            delivery.title shouldNotContain reason
            delivery.body shouldNotContain reason
            delivery.data.values.none { reason in it } shouldBe true
            outboxes.findAll().count { it.deliveryId == delivery.id } shouldBe 1
            transaction.executeWithoutResult { publisher.publishEvent(event(f, original)) }
            notifications.findAll().count { it.clubId == f.club.id } shouldBe 1
            deliveries.findAll().count { it.clubId == f.club.id } shouldBe 1
        }

        "직접 벌점과 자동 전환 벌점, 경고 수정·삭제는 추가 알림을 만들지 않는다" {
            val f = ready()
            assign(f, type = PenaltyType.PENALTY)
            notifications.findAll().none { it.clubId == f.club.id } shouldBe true
            repeat(ClubMember.WARNING_TO_PENALTY_THRESHOLD) { assign(f) }
            val warnings = rows(f).filter { it.penaltyType == PenaltyType.WARNING }
            rows(f).count { it.penaltyType == PenaltyType.PENALTY } shouldBe 2
            notifications
                .findAll()
                .filter { it.clubId == f.club.id }
                .map { it.referenceId }
                .toSet() shouldBe
                warnings.map { it.id }.toSet()
            update.update(f.club.id, f.adminId, UpdatePenaltyRequest(warnings.first().id, "변경된 사유"))
            delete.delete(f.club.id, f.adminId, warnings.first().id)
            notifications.findAll().count { it.clubId == f.club.id } shouldBe warnings.size
            deliveries.findAll().count { it.clubId == f.club.id } shouldBe warnings.size
        }

        "원본 트랜잭션이 롤백되면 경고 집계와 앱 알림·발송 작업이 함께 사라진다" {
            val f = ready()
            val outboxCount = outboxes.count()
            shouldThrow<IllegalStateException> {
                transaction.executeWithoutResult {
                    assign(f)
                    error("rollback")
                }
            }
            rows(f).size shouldBe 0
            members.findById(f.roster.getValue("normal").id).orElseThrow().warningCount shouldBe 0
            notifications.findAll().none { it.clubId == f.club.id } shouldBe true
            deliveries.findAll().none { it.clubId == f.club.id } shouldBe true
            outboxes.count() shouldBe outboxCount
        }

        "legacy도 동일 이벤트를 한 번만 생성하고 유효 경고는 보내되 삭제된 경고는 발송하지 않는다" {
            val f = ready()
            val original = penalties.save(Penalty(f.roster.getValue("normal"), f.cardinal, reason, PenaltyType.WARNING))
            val forwarded = slot<NotificationCreatedEvent>()
            val legacyPublisher = mockk<ApplicationEventPublisher>()
            every { legacyPublisher.publishEvent(capture(forwarded)) } returns Unit
            val listener =
                CreateWarningNotificationEventListener(
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
            repeat(2) { transaction.executeWithoutResult { listener.handle(event(f, original)) } }
            notifications.findAll().count { it.clubId == f.club.id } shouldBe 1
            verify(exactly = 1) { legacyPublisher.publishEvent(any<NotificationCreatedEvent>()) }
            deliveries.findAll().none { it.clubId == f.club.id } shouldBe true
            registerToken(f.roster.getValue("normal"))
            val sender = mockk<PushNotificationSenderPort>(relaxed = true)
            val push =
                SendPushNotificationEventListener(tokens, sender, tokenUseCase, recipients, duesPolicy, warningPolicy)
            push.handle(forwarded.captured)
            verify(exactly = 1) {
                sender.sendMulticast(
                    match {
                        reason !in it.title && reason !in it.body &&
                            it.data.values.none { value -> reason in value }
                    },
                )
            }
            delete.delete(f.club.id, f.adminId, original.id)
            clearMocks(sender, answers = false)
            push.handle(forwarded.captured)
            verify(exactly = 0) { sender.sendMulticast(any()) }
            notifications.findAll().count { it.clubId == f.club.id } shouldBe 1
        }

        "durable 재시도 전 삭제·회원 퇴출·기능 비활성화를 재검증하고 잘못된 소유자나 원본 유형을 거부한다" {
            val f = ready()
            assign(f, listOf("normal", "other", "admin"))
            f.roster.values
                .take(3)
                .forEach { registerToken(it) }
            val original = rows(f).single { it.clubMember.id == f.roster.getValue("normal").id }
            val normalId =
                f.roster
                    .getValue("normal")
                    .user.id
            warningPolicy.filterEligibleUserIds(
                NotificationType.WARNING_ASSIGNED,
                f.club.id,
                original.id,
                listOf(normalId),
            ) shouldBe
                listOf(normalId)
            warningPolicy.filterEligibleUserIds(
                NotificationType.WARNING_ASSIGNED,
                f.club.id,
                original.id,
                listOf(f.adminId),
            ) shouldBe
                emptyList()
            warningPolicy.filterEligibleUserIds(
                NotificationType.WARNING_ASSIGNED,
                f.club.id + 1,
                original.id,
                listOf(normalId),
            ) shouldBe
                emptyList()
            val directPenalty =
                penalties.save(
                    Penalty(f.roster.getValue("normal"), f.cardinal, reason, PenaltyType.PENALTY),
                )
            warningPolicy.filterEligibleUserIds(
                NotificationType.WARNING_ASSIGNED,
                f.club.id,
                directPenalty.id,
                listOf(normalId),
            ) shouldBe
                emptyList()
            val normal = deliveries.findAll().single { it.clubId == f.club.id && it.userId == normalId }
            val message = NotificationQueueMessage(normal.id, normal.eventId)
            val attempt = checkNotNull(management.prepare(message).attempt)
            management.complete(
                attempt,
                PushNotificationResult(
                    outcomes =
                        attempt.targets.map {
                            PushTargetOutcome(it.target.token, PushTargetStatus.RETRYABLE, "UNAVAILABLE")
                        },
                ),
            )
            delete.delete(f.club.id, f.adminId, original.id)
            transaction.executeWithoutResult { members.findById(f.roster.getValue("other").id).orElseThrow().ban() }
            every { clock.instant() } returns base.plusSeconds(20)
            management.prepare(message).action shouldBe NotificationDeliveryAction.ACK
            deliveries.findById(normal.id).orElseThrow().lastErrorCode shouldBe "WARNING_NO_LONGER_VALID"
            val other =
                deliveries.findAll().single {
                    it.clubId == f.club.id &&
                        it.userId ==
                        f.roster
                            .getValue("other")
                            .user.id
                }
            management.prepare(NotificationQueueMessage(other.id, other.eventId)).action shouldBe
                NotificationDeliveryAction.ACK
            deliveries.findById(other.id).orElseThrow().lastErrorCode shouldBe "RECIPIENT_NO_LONGER_ELIGIBLE"
            transaction.executeWithoutResult {
                ReflectionTestUtils.setField(clubs.findById(f.club.id).orElseThrow(), "warningEnabled", false)
            }
            val admin = deliveries.findAll().single { it.clubId == f.club.id && it.userId == f.adminId }
            management.prepare(NotificationQueueMessage(admin.id, admin.eventId)).action shouldBe
                NotificationDeliveryAction.ACK
            deliveries.findById(admin.id).orElseThrow().lastErrorCode shouldBe "RECIPIENT_NO_LONGER_ELIGIBLE"
            notifications.findAll().count { it.clubId == f.club.id } shouldBe 3
        }
    })
