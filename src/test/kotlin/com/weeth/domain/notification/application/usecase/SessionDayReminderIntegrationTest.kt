package com.weeth.domain.notification.application.usecase

import com.ninjasquad.springmockk.MockkBean
import com.weeth.config.TestContainersConfig
import com.weeth.domain.cardinal.domain.repository.CardinalRepository
import com.weeth.domain.cardinal.fixture.CardinalTestFixture
import com.weeth.domain.club.domain.entity.Club
import com.weeth.domain.club.domain.enums.MemberRole
import com.weeth.domain.club.domain.repository.ClubMemberCardinalRepository
import com.weeth.domain.club.domain.repository.ClubMemberRepository
import com.weeth.domain.club.domain.repository.ClubRepository
import com.weeth.domain.club.fixture.ClubMemberCardinalTestFixture
import com.weeth.domain.notification.application.dispatch.SessionDayReminderScheduler
import com.weeth.domain.notification.application.usecase.command.CreateNotificationUseCase
import com.weeth.domain.notification.application.usecase.command.EnqueueNotificationDeliveryUseCase
import com.weeth.domain.notification.application.usecase.command.GenerateSessionDayReminderUseCase
import com.weeth.domain.notification.application.usecase.command.ManageNotificationDeliveryUseCase
import com.weeth.domain.notification.domain.entity.NotificationPreference
import com.weeth.domain.notification.domain.entity.NotificationToken
import com.weeth.domain.notification.domain.enums.NotificationCategory
import com.weeth.domain.notification.domain.enums.NotificationDeliveryAction
import com.weeth.domain.notification.domain.enums.NotificationDeliveryStatus
import com.weeth.domain.notification.domain.enums.NotificationPlatform
import com.weeth.domain.notification.domain.enums.PushTargetStatus
import com.weeth.domain.notification.domain.repository.NotificationDeliveryRepository
import com.weeth.domain.notification.domain.repository.NotificationPreferenceRepository
import com.weeth.domain.notification.domain.repository.NotificationScheduleRunRepository
import com.weeth.domain.notification.domain.repository.NotificationTokenRepository
import com.weeth.domain.notification.domain.repository.UserNotificationRepository
import com.weeth.domain.notification.domain.service.DuesNotificationPolicy
import com.weeth.domain.notification.domain.service.NotificationRecipientPolicy
import com.weeth.domain.notification.domain.service.NotificationRetryPolicy
import com.weeth.domain.notification.domain.service.NotificationTemplateRenderer
import com.weeth.domain.notification.domain.service.SessionDayReminderPolicy
import com.weeth.domain.notification.domain.vo.NotificationQueueMessage
import com.weeth.domain.notification.domain.vo.PushNotificationResult
import com.weeth.domain.notification.domain.vo.PushTargetOutcome
import com.weeth.domain.notification.fixture.NotificationIntegrationTestFixture.saveMember
import com.weeth.domain.notification.fixture.NotificationPreferenceTestFixture
import com.weeth.domain.notification.infrastructure.CodeNotificationTemplateProvider
import com.weeth.domain.notification.infrastructure.JdbcUserNotificationBulkWriterAdapter
import com.weeth.domain.session.domain.entity.Session
import com.weeth.domain.session.domain.repository.SessionRepository
import com.weeth.domain.user.domain.entity.User
import com.weeth.domain.user.domain.repository.UserRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@DataJpaTest(properties = ["notification.dispatch.enabled=true"])
@Import(
    TestContainersConfig::class,
    GenerateSessionDayReminderUseCase::class,
    CreateNotificationUseCase::class,
    EnqueueNotificationDeliveryUseCase::class,
    ManageNotificationDeliveryUseCase::class,
    NotificationRetryPolicy::class,
    SessionDayReminderPolicy::class,
    DuesNotificationPolicy::class,
    NotificationRecipientPolicy::class,
    NotificationTemplateRenderer::class,
    CodeNotificationTemplateProvider::class,
    JdbcUserNotificationBulkWriterAdapter::class,
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SessionDayReminderIntegrationTest(
    private val generator: GenerateSessionDayReminderUseCase,
    private val policy: SessionDayReminderPolicy,
    private val management: ManageNotificationDeliveryUseCase,
    private val clubs: ClubRepository,
    private val users: UserRepository,
    private val members: ClubMemberRepository,
    private val cardinals: CardinalRepository,
    private val memberCardinals: ClubMemberCardinalRepository,
    private val sessions: SessionRepository,
    private val deliveries: NotificationDeliveryRepository,
    private val notifications: UserNotificationRepository,
    private val runs: NotificationScheduleRunRepository,
    private val tokens: NotificationTokenRepository,
    private val preferences: NotificationPreferenceRepository,
    private val jdbc: JdbcTemplate,
    private val transactionManager: PlatformTransactionManager,
    @MockkBean private val clock: Clock,
) : StringSpec({
        val noon = LocalDateTime.of(2026, 11, 10, 12, 0)
        val zone = ZoneId.of("Asia/Seoul")
        val transaction = TransactionTemplate(transactionManager)

        fun at(time: LocalDateTime) {
            every { clock.zone } returns zone
            every { clock.instant() } returns time.atZone(zone).toInstant()
        }

        data class Fixture(
            val club: Club,
            val author: User,
            val member: User,
            val first: Session,
            val second: Session,
        )

        fun fixture(): Fixture {
            val club = clubs.save(NotificationPreferenceTestFixture.createClub())
            val admin = members.saveMember(club, users, role = MemberRole.ADMIN)
            val active = members.saveMember(club, users)
            val author = admin.user
            val member = active.user
            val firstCardinal = cardinals.save(CardinalTestFixture.createCardinal(club = club, cardinalNumber = 1))
            val secondCardinal = cardinals.save(CardinalTestFixture.createCardinal(club = club, cardinalNumber = 2))
            memberCardinals.save(ClubMemberCardinalTestFixture.create(admin, firstCardinal))
            memberCardinals.save(ClubMemberCardinalTestFixture.create(active, firstCardinal))
            memberCardinals.save(ClubMemberCardinalTestFixture.create(active, secondCardinal))
            val optedOutMember = members.saveMember(club, users)
            val optedOut = optedOutMember.user
            memberCardinals.save(ClubMemberCardinalTestFixture.create(optedOutMember, firstCardinal))
            preferences.save(NotificationPreference.create(optedOut.id, club.id, NotificationCategory.SCHEDULE, false))
            val first =
                sessions.save(
                    Session.create(club, "오후 세션", null, null, 1, noon.plusHours(2), noon.plusHours(3), author),
                )
            val second =
                sessions.save(
                    Session.create(club, "저녁 세션", null, null, 2, noon.plusHours(6), noon.plusHours(7), author),
                )
            sessions.save(Session.create(club, "끝난 세션", null, null, 1, noon.minusHours(2), noon.minusHours(1), author))
            sessions.save(
                Session.create(club, "닫힌 세션", null, null, 1, noon.plusHours(1), noon.plusHours(2), author).apply {
                    close()
                },
            )
            sessions.save(Session.create(club, "전날 세션", null, null, 1, noon.minusDays(1), noon.plusHours(1), author))
            val late =
                sessions.save(
                    Session.create(club, "정오 이후 등록", null, null, 1, noon.plusHours(3), noon.plusHours(4), author),
                )
            jdbc.update("UPDATE session SET created_at = ? WHERE club_id = ?", noon.minusHours(1), club.id)
            jdbc.update("UPDATE session SET created_at = ? WHERE id = ?", noon.plusMinutes(1), late.id)
            return Fixture(club, author, member, first, second)
        }
        beforeTest { at(noon) }

        "정오 창에서만 생성하고 회원별 보이는 세션을 한 번 요약하며 작성자도 포함한다" {
            val f = fixture()
            at(noon.minusSeconds(1))
            generator.claim(f.club.id) shouldBe null
            at(noon)
            SessionDayReminderScheduler(generator, sessions, policy, clock).runOnce()
            deliveries.findAll().none { it.clubId == f.club.id } shouldBe true
            transaction.executeWithoutResult {
                val first = sessions.findById(f.first.id).orElseThrow()
                first.updateInfo("긴 제목".repeat(60), null, null, first.start, first.end, f.author)
                sessions.save(first)
            }
            SessionDayReminderScheduler(generator, sessions, policy, clock, true).runOnce()
            generator.claim(f.club.id) shouldBe null
            val jobs = deliveries.findAll().filter { it.clubId == f.club.id }
            jobs.size shouldBe 2
            jobs.single { it.userId == f.author.id }.sourceSessionIds shouldBe listOf(f.first.id)
            jobs.single { it.userId == f.member.id }.sourceSessionIds shouldBe listOf(f.first.id, f.second.id)
            jobs.all { it.expiresAt == noon.plusMinutes(30) } shouldBe true
            jobs.single { it.userId == f.member.id }.body shouldContain "14:00"
            jobs.single { it.userId == f.member.id }.body shouldContain "외 1개"
            (jobs.single { it.userId == f.member.id }.body.length <= 255) shouldBe true
            notifications.findAll().count { it.clubId == f.club.id } shouldBe 2
            at(noon.plusMinutes(30))
            val another = fixture()
            generator.claim(another.club.id) shouldBe null
        }

        "동시 선점은 하나만 성공하고 롤백 후 만료 lease를 복구한다" {
            val f = fixture()
            val executor = Executors.newFixedThreadPool(2)
            val start = CountDownLatch(1)
            val claim =
                try {
                    val calls =
                        (1..2).map {
                            executor.submit<com.weeth.domain.notification.domain.vo.NotificationScheduleClaim?> {
                                start.await()
                                generator.claim(f.club.id)
                            }
                        }
                    start.countDown()
                    calls.map { it.get(10, TimeUnit.SECONDS) }.filterNotNull().single()
                } finally {
                    executor.shutdownNow()
                }
            shouldThrow<IllegalStateException> {
                transaction.executeWithoutResult {
                    generator.generate(claim)
                    error("rollback")
                }
            }
            notifications.findAll().none { it.clubId == f.club.id } shouldBe true
            at(noon.plusSeconds(61))
            val retry = requireNotNull(generator.claim(f.club.id))
            generator.generate(claim) shouldBe false
            generator.generate(retry) shouldBe true
            deliveries.findAll().count { it.clubId == f.club.id } shouldBe 2
            runs.findAll().count { it.clubId == f.club.id } shouldBe 1
        }

        "발송 직전 남은 세션만 재렌더링하고 재시도는 12시30분에 만료한다" {
            val f = fixture()
            generator.generate(requireNotNull(generator.claim(f.club.id)))
            val delivery = deliveries.findAll().single { it.clubId == f.club.id && it.userId == f.member.id }
            tokens.save(NotificationToken.create(f.member, "reminder-${f.member.id}", NotificationPlatform.WEB, noon))
            transaction.executeWithoutResult {
                val first = sessions.findById(f.first.id).orElseThrow()
                first.updateInfo("변경된 세션", null, null, noon.plusHours(3), noon.plusHours(4), f.author)
                sessions.save(first)
                sessions.deleteById(f.second.id)
                sessions.save(
                    Session.create(f.club, "새로 추가", null, null, 1, noon.plusHours(5), noon.plusHours(6), f.author),
                )
            }
            at(noon.plusMinutes(5))
            val message = NotificationQueueMessage(delivery.id, delivery.eventId)
            val attempt = requireNotNull(management.prepare(message).attempt)
            attempt.command.body shouldContain "변경된 세션"
            attempt.command.body shouldContain "15:00"
            attempt.command.body.contains("저녁 세션") shouldBe false
            attempt.command.body.contains("새로 추가") shouldBe false
            deliveries.findById(delivery.id).orElseThrow().body shouldBe delivery.body
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
            at(noon.plusMinutes(30).minusSeconds(1))
            val lastAttempt = requireNotNull(management.prepare(message).attempt)
            at(noon.plusMinutes(30))
            management
                .complete(
                    lastAttempt,
                    PushNotificationResult(
                        outcomes =
                            listOf(
                                PushTargetOutcome(
                                    lastAttempt.targets
                                        .single()
                                        .target.token,
                                    PushTargetStatus.RETRYABLE,
                                    "UNAVAILABLE",
                                ),
                            ),
                    ),
                ).action shouldBe NotificationDeliveryAction.ACK
            management.prepare(message).action shouldBe NotificationDeliveryAction.ACK
            val expired = deliveries.findById(delivery.id).orElseThrow()
            expired.status shouldBe NotificationDeliveryStatus.SKIPPED
            expired.lastErrorCode shouldBe "REMINDER_EXPIRED"
        }

        "모든 세션이 무효가 되면 발송하지 않고 skip한다" {
            val f = fixture()
            generator.generate(requireNotNull(generator.claim(f.club.id)))
            val delivery = deliveries.findAll().single { it.clubId == f.club.id && it.userId == f.author.id }
            sessions.deleteById(f.first.id)
            management.prepare(NotificationQueueMessage(delivery.id, delivery.eventId)).action shouldBe
                NotificationDeliveryAction.ACK
            deliveries.findById(delivery.id).orElseThrow().lastErrorCode shouldBe "REMINDER_NO_VALID_SESSION"
        }
    })
