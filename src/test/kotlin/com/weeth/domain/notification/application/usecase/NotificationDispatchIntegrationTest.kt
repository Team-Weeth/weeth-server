package com.weeth.domain.notification.application.usecase

import com.ninjasquad.springmockk.MockkBean
import com.weeth.config.TestContainersConfig
import com.weeth.domain.club.domain.repository.ClubMemberRepository
import com.weeth.domain.club.domain.repository.ClubRepository
import com.weeth.domain.club.fixture.ClubMemberTestFixture
import com.weeth.domain.notification.application.dispatch.NotificationDeliveryProcessor
import com.weeth.domain.notification.application.dispatch.NotificationOutboxRelay
import com.weeth.domain.notification.application.usecase.command.CreateNotificationUseCase
import com.weeth.domain.notification.application.usecase.command.EnqueueNotificationDeliveryUseCase
import com.weeth.domain.notification.application.usecase.command.ManageNotificationDeliveryUseCase
import com.weeth.domain.notification.application.usecase.command.ManageNotificationOutboxUseCase
import com.weeth.domain.notification.domain.entity.NotificationDelivery
import com.weeth.domain.notification.domain.entity.NotificationSetting
import com.weeth.domain.notification.domain.entity.NotificationToken
import com.weeth.domain.notification.domain.enums.NotificationDeliveryAction
import com.weeth.domain.notification.domain.enums.NotificationDeliveryStatus
import com.weeth.domain.notification.domain.enums.NotificationOutboxStatus
import com.weeth.domain.notification.domain.enums.NotificationPlatform
import com.weeth.domain.notification.domain.enums.NotificationTargetStatus
import com.weeth.domain.notification.domain.enums.PushTargetStatus
import com.weeth.domain.notification.domain.port.NotificationQueuePort
import com.weeth.domain.notification.domain.port.PushNotificationSenderPort
import com.weeth.domain.notification.domain.repository.NotificationDeliveryRepository
import com.weeth.domain.notification.domain.repository.NotificationDeliveryTargetRepository
import com.weeth.domain.notification.domain.repository.NotificationEventRepository
import com.weeth.domain.notification.domain.repository.NotificationOutboxRepository
import com.weeth.domain.notification.domain.repository.NotificationQueueIncidentRepository
import com.weeth.domain.notification.domain.repository.NotificationSettingRepository
import com.weeth.domain.notification.domain.repository.NotificationTokenRepository
import com.weeth.domain.notification.domain.repository.UserNotificationRepository
import com.weeth.domain.notification.domain.service.NotificationRecipientPolicy
import com.weeth.domain.notification.domain.service.NotificationRetryPolicy
import com.weeth.domain.notification.domain.service.NotificationTemplateRenderer
import com.weeth.domain.notification.domain.vo.NotificationQueueMessage
import com.weeth.domain.notification.domain.vo.NotificationQueueReceipt
import com.weeth.domain.notification.domain.vo.PushNotificationResult
import com.weeth.domain.notification.domain.vo.PushTargetOutcome
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
import io.mockk.justRun
import io.mockk.mockk
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@DataJpaTest(
    properties = [
        "notification.dispatch.enabled=true",
        "notification.dispatch.queue-url=http://localhost/queue",
        "notification.dispatch.dlq-url=http://localhost/dlq",
        "notification.dispatch.max-attempts=2",
    ],
)
@Import(
    TestContainersConfig::class,
    CreateNotificationUseCase::class,
    EnqueueNotificationDeliveryUseCase::class,
    ManageNotificationDeliveryUseCase::class,
    ManageNotificationOutboxUseCase::class,
    NotificationRetryPolicy::class,
    NotificationRecipientPolicy::class,
    NotificationTemplateRenderer::class,
    CodeNotificationTemplateProvider::class,
    JdbcUserNotificationBulkWriterAdapter::class,
    NotificationEventProbe::class,
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class NotificationDispatchIntegrationTest(
    private val create: CreateNotificationUseCase,
    private val management: ManageNotificationDeliveryUseCase,
    private val outboxManagement: ManageNotificationOutboxUseCase,
    private val deliveries: NotificationDeliveryRepository,
    private val outboxes: NotificationOutboxRepository,
    private val targets: NotificationDeliveryTargetRepository,
    private val events: NotificationEventRepository,
    private val incidents: NotificationQueueIncidentRepository,
    private val notifications: UserNotificationRepository,
    private val clubs: ClubRepository,
    private val users: UserRepository,
    private val members: ClubMemberRepository,
    private val settings: NotificationSettingRepository,
    private val tokens: NotificationTokenRepository,
    private val probe: NotificationEventProbe,
    private val transactions: PlatformTransactionManager,
    @MockkBean private val clock: Clock,
) : StringSpec({
        val base = Instant.parse("2026-11-08T03:00:00Z")

        fun advance(seconds: Long) {
            every { clock.instant() } returns base.plusSeconds(seconds)
        }

        fun job(): NotificationDelivery {
            val club = clubs.save(NotificationPreferenceTestFixture.createClub())
            val user = users.save(NotificationPreferenceTestFixture.createUser())
            members.save(ClubMemberTestFixture.createActiveMember(club = club, user = user))
            tokens.save(
                NotificationToken.create(user, "token-" + user.id, NotificationPlatform.WEB, LocalDateTime.now(clock)),
            )
            create.execute(NotificationRequestTestFixture.createNotice(club.id), listOf(user.id))
            return deliveries.findAll().single { it.clubId == club.id }
        }

        fun message(delivery: NotificationDelivery) = NotificationQueueMessage(delivery.id, delivery.eventId)

        fun retryResult(attempt: com.weeth.domain.notification.domain.vo.NotificationDeliveryAttempt) =
            PushNotificationResult(
                outcomes =
                    attempt.targets.map {
                        PushTargetOutcome(it.target.token, PushTargetStatus.RETRYABLE, "UNAVAILABLE")
                    },
            )

        fun successResult(attempt: com.weeth.domain.notification.domain.vo.NotificationDeliveryAttempt) =
            PushNotificationResult(
                outcomes =
                    attempt.targets.map {
                        PushTargetOutcome(it.target.token, PushTargetStatus.SUCCESS)
                    },
            )
        beforeTest {
            every { clock.zone } returns ZoneId.of("Asia/Seoul")
            advance(0)
            targets.deleteAllInBatch()
            outboxes.deleteAllInBatch()
            deliveries.deleteAllInBatch()
            events.deleteAllInBatch()
            incidents.deleteAllInBatch()
            notifications.deleteAllInBatch()
            probe.events.clear()
        }

        "원본 트랜잭션 롤백은 선점·알림·Delivery·Outbox를 모두 되돌린다" {
            shouldThrow<IllegalStateException> {
                TransactionTemplate(transactions).executeWithoutResult {
                    job()
                    error("rollback")
                }
            }
            events.count() shouldBe 0
            deliveries.count() shouldBe 0
            outboxes.count() shouldBe 0
            notifications.count() shouldBe 0
            probe.events.isEmpty() shouldBe true
        }

        "동일 논리 이벤트의 동시 재발행도 알림·Delivery·Outbox를 한 번만 생성한다" {
            val club = clubs.save(NotificationPreferenceTestFixture.createClub())
            val user = users.save(NotificationPreferenceTestFixture.createUser())
            members.save(ClubMemberTestFixture.createActiveMember(club = club, user = user))
            val executor = Executors.newFixedThreadPool(2)
            val start = CountDownLatch(1)
            try {
                val calls =
                    (1..2).map {
                        executor.submit {
                            start.await()
                            create.execute(
                                NotificationRequestTestFixture.createNotice(club.id),
                                listOf(user.id),
                            )
                        }
                    }
                start.countDown()
                calls.forEach { it.get(10, TimeUnit.SECONDS) }
            } finally {
                executor.shutdownNow()
            }
            deliveries.count() shouldBe 1
            outboxes.count() shouldBe 1
            notifications.count() shouldBe 1
            probe.events.isEmpty() shouldBe true
        }

        "두 워커의 동시 선점은 하나의 READY와 하나의 RETRY로 분리된다" {
            val delivery = job()
            val executor = Executors.newFixedThreadPool(2)
            val start = CountDownLatch(1)
            try {
                val calls =
                    (1..2).map {
                        executor.submit<com.weeth.domain.notification.domain.vo.NotificationDeliveryPreparation> {
                            start.await()
                            management.prepare(message(delivery))
                        }
                    }
                start.countDown()
                val results = calls.map { it.get(10, TimeUnit.SECONDS) }
                results.count { it.action == NotificationDeliveryAction.READY } shouldBe 1
                results.count { it.action == NotificationDeliveryAction.RETRY } shouldBe 1
                deliveries.findById(delivery.id).orElseThrow().attemptCount shouldBe 1
            } finally {
                executor.shutdownNow()
            }
        }

        "두 Relay는 잠긴 Outbox를 건너뛰며 같은 작업을 선점하지 않는다" {
            job()
            val executor = Executors.newSingleThreadExecutor()
            try {
                TransactionTemplate(transactions).executeWithoutResult {
                    outboxManagement.claimBatch(1).size shouldBe 1
                    executor
                        .submit<Int> { outboxManagement.claimBatch(1).size }
                        .get(5, TimeUnit.SECONDS) shouldBe 0
                }
            } finally {
                executor.shutdownNow()
            }
        }

        "Relay는 미커밋 작업을 읽지 않으며 SQS 장애 후 게시를 재개한다" {
            val executor = Executors.newSingleThreadExecutor()
            try {
                TransactionTemplate(transactions).executeWithoutResult {
                    job()
                    executor
                        .submit<List<com.weeth.domain.notification.domain.vo.NotificationOutboxDispatch>> {
                            outboxManagement.claimBatch(10)
                        }.get(5, TimeUnit.SECONDS)
                        .isEmpty() shouldBe
                        true
                }
            } finally {
                executor.shutdownNow()
            }
            val queue = mockk<NotificationQueuePort>()
            every { queue.publish(any()) } answers {
                TransactionSynchronizationManager.isActualTransactionActive() shouldBe false
                throw IllegalStateException("SQS unavailable")
            }
            NotificationOutboxRelay(outboxManagement, queue).runOnce()
            outboxes.findAll().single().status shouldBe NotificationOutboxStatus.PENDING
            deliveries.findAll().single().status shouldBe NotificationDeliveryStatus.PENDING
            advance(10)
            every { queue.publish(any()) } answers
                { firstArg<List<NotificationQueueMessage>>().map { it.deliveryId }.toSet() }
            NotificationOutboxRelay(outboxManagement, queue).runOnce()
            outboxes.findAll().single().status shouldBe NotificationOutboxStatus.PUBLISHED
        }

        "부분 성공 토큰은 재시도에서 제외하고 완료 후 중복 수신은 ACK한다" {
            val delivery = job()
            val user = users.findById(delivery.userId).orElseThrow()
            tokens.save(
                NotificationToken.create(user, "second-" + user.id, NotificationPlatform.WEB, LocalDateTime.now(clock)),
            )
            val first = requireNotNull(management.prepare(message(delivery)).attempt)
            val outcomes =
                first.targets.mapIndexed { index, target ->
                    PushTargetOutcome(
                        target.target.token,
                        if (index ==
                            0
                        ) {
                            PushTargetStatus.SUCCESS
                        } else {
                            PushTargetStatus.RETRYABLE
                        },
                    )
                }
            management.complete(first, PushNotificationResult(outcomes = outcomes)).action shouldBe
                NotificationDeliveryAction.RETRY
            management.prepare(message(delivery)).action shouldBe NotificationDeliveryAction.RETRY
            advance(20)
            val second = requireNotNull(management.prepare(message(delivery)).attempt)
            second.targets.size shouldBe 1
            second.targets.single().id shouldBe first.targets[1].id
            management.complete(second, successResult(second)).action shouldBe NotificationDeliveryAction.ACK
            management.prepare(message(delivery)).action shouldBe NotificationDeliveryAction.ACK
            targets.findAll().all { it.status == NotificationTargetStatus.SUCCEEDED } shouldBe true
        }

        "만료된 lease의 결과는 새 워커의 상태와 토큰 성공 기록을 덮어쓰지 않는다" {
            val delivery = job()
            val first = requireNotNull(management.prepare(message(delivery)).attempt)
            management.prepare(message(delivery)).action shouldBe NotificationDeliveryAction.RETRY
            advance(61)
            val second = requireNotNull(management.prepare(message(delivery)).attempt)
            management.complete(first, successResult(first)).action shouldBe NotificationDeliveryAction.RETRY
            targets.findAll().single().status shouldBe NotificationTargetStatus.PENDING
            management.complete(second, successResult(second)).action shouldBe NotificationDeliveryAction.ACK
        }

        "heartbeat는 lease를 연장하고 발송은 DB 트랜잭션 밖에서 완료한다" {
            val delivery = job()
            val attempt = requireNotNull(management.prepare(message(delivery)).attempt)
            advance(30)
            management.extendLease(attempt) shouldBe true
            advance(61)
            management.prepare(message(delivery)).action shouldBe NotificationDeliveryAction.RETRY
            management.complete(attempt, successResult(attempt)).action shouldBe NotificationDeliveryAction.ACK
            val another = job()
            val sender = mockk<PushNotificationSenderPort>()
            val queue = mockk<NotificationQueuePort>()
            every { sender.sendMulticast(any()) } answers {
                TransactionSynchronizationManager.isActualTransactionActive() shouldBe false
                PushNotificationResult(
                    outcomes =
                        firstArg<com.weeth.domain.notification.domain.vo.PushNotificationCommand>()
                            .targets
                            .map {
                                PushTargetOutcome(it.token, PushTargetStatus.SUCCESS)
                            },
                )
            }
            justRun { queue.changeVisibility(any(), any()) }
            every { queue.delete(any()) } answers {
                TransactionSynchronizationManager.isActualTransactionActive() shouldBe false
                deliveries.findById(another.id).orElseThrow().status shouldBe NotificationDeliveryStatus.SUCCEEDED
            }
            val io = Executors.newFixedThreadPool(1)
            val heartbeat = Executors.newScheduledThreadPool(1)
            try {
                NotificationDeliveryProcessor(
                    management,
                    queue,
                    sender,
                    io,
                    heartbeat,
                ).process(NotificationQueueReceipt("message", "receipt", message(another)))
            } finally {
                io.shutdownNow()
                heartbeat.shutdownNow()
            }
            deliveries.findById(another.id).orElseThrow().status shouldBe NotificationDeliveryStatus.SUCCEEDED
        }

        "발송 전 OFF는 작업을 skip하고 기존 알림함을 유지한다" {
            val delivery = job()
            settings.save(NotificationSetting.create(delivery.userId, delivery.clubId).apply { updateMaster(false) })
            management.prepare(message(delivery)).action shouldBe NotificationDeliveryAction.ACK
            deliveries.findById(delivery.id).orElseThrow().status shouldBe NotificationDeliveryStatus.SKIPPED
            notifications.count() shouldBe 1
        }

        "토큰 재등록·소유자 변경은 invalid 결과로 새 소유자의 토큰을 해제하지 않는다" {
            val delivery = job()
            val first = requireNotNull(management.prepare(message(delivery)).attempt)
            val other = users.save(NotificationPreferenceTestFixture.createUser())
            val token =
                first.targets
                    .single()
                    .target.token
            TransactionTemplate(transactions).executeWithoutResult {
                tokens.registerToken(
                    other.id,
                    token,
                    NotificationPlatform.WEB.name,
                    LocalDateTime.now(clock).plusSeconds(1),
                )
            }
            management.complete(
                first,
                PushNotificationResult(
                    outcomes = listOf(PushTargetOutcome(token, PushTargetStatus.INVALID_TOKEN, "UNREGISTERED")),
                ),
            )
            tokens.findByToken(token)?.isActive shouldBe true
            tokens.findByToken(token)?.user?.id shouldBe other.id
        }

        "재시도는 새 토큰을 추가하지 않으며 등록시각이 바뀐 기존 토큰은 제외한다" {
            val delivery = job()
            val first = requireNotNull(management.prepare(message(delivery)).attempt)
            management.complete(first, retryResult(first))
            val user = users.findById(delivery.userId).orElseThrow()
            tokens.save(
                NotificationToken.create(
                    user,
                    "new-" + user.id,
                    NotificationPlatform.WEB,
                    LocalDateTime.now(clock).plusSeconds(1),
                ),
            )
            TransactionTemplate(transactions).executeWithoutResult {
                tokens.registerToken(
                    user.id,
                    first.targets
                        .single()
                        .target.token,
                    NotificationPlatform.WEB.name,
                    LocalDateTime.now(clock).plusSeconds(1),
                )
            }
            advance(20)
            management.prepare(message(delivery)).action shouldBe NotificationDeliveryAction.ACK
            targets.count() shouldBe 1
            targets.findAll().single().status shouldBe NotificationTargetStatus.SKIPPED
        }

        "재시도 소진·DLQ 감사·명시적 재처리와 장기 미완료 재게시를 추적한다" {
            val delivery = job()
            val first = requireNotNull(management.prepare(message(delivery)).attempt)
            management.complete(first, retryResult(first))
            advance(20)
            val second = requireNotNull(management.prepare(message(delivery)).attempt)
            management.complete(second, retryResult(second)).action shouldBe NotificationDeliveryAction.AWAIT_DLQ
            val receipt = NotificationQueueReceipt("dlq-message", "receipt", message(delivery), 10, true)
            management.recordDeadLetter(receipt)
            management.recordDeadLetter(receipt)
            incidents.count() shouldBe 1
            deliveries.findById(delivery.id).orElseThrow().deadLetteredAt shouldBe LocalDateTime.now(clock)
            management.requeueFailedDelivery(delivery.id)
            deliveries.findById(delivery.id).orElseThrow().attemptCount shouldBe 0
            val dispatch = outboxManagement.claimBatch(10).single()
            outboxManagement.completePublish(dispatch, true)
            advance(7200)
            outboxManagement.reconcileStalePublished() shouldBe 1
            outboxes.findAll().single().status shouldBe NotificationOutboxStatus.PENDING
            management.recordDeadLetter(NotificationQueueReceipt("malformed", "receipt", null, 10, true))
            incidents.findById("malformed").orElseThrow().reason shouldBe "MALFORMED_MESSAGE"
        }
    })
