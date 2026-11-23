package com.weeth.domain.notification.application.usecase

import com.weeth.config.TestContainersConfig
import com.weeth.domain.cardinal.domain.repository.CardinalRepository
import com.weeth.domain.cardinal.fixture.CardinalTestFixture
import com.weeth.domain.club.domain.enums.MemberRole
import com.weeth.domain.club.domain.enums.MemberStatus
import com.weeth.domain.club.domain.repository.ClubMemberCardinalRepository
import com.weeth.domain.club.domain.repository.ClubMemberRepository
import com.weeth.domain.club.domain.repository.ClubRepository
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.club.domain.service.ClubPermissionPolicy
import com.weeth.domain.notification.application.event.CreateScheduleNotificationEventListener
import com.weeth.domain.notification.application.usecase.command.CreateNotificationUseCase
import com.weeth.domain.notification.application.usecase.command.EnqueueNotificationDeliveryUseCase
import com.weeth.domain.notification.domain.entity.NotificationPreference
import com.weeth.domain.notification.domain.entity.NotificationSetting
import com.weeth.domain.notification.domain.enums.NotificationCategory
import com.weeth.domain.notification.domain.enums.NotificationType
import com.weeth.domain.notification.domain.repository.NotificationDeliveryRepository
import com.weeth.domain.notification.domain.repository.NotificationOutboxRepository
import com.weeth.domain.notification.domain.repository.NotificationPreferenceRepository
import com.weeth.domain.notification.domain.repository.NotificationSettingRepository
import com.weeth.domain.notification.domain.repository.UserNotificationRepository
import com.weeth.domain.notification.domain.service.NotificationRecipientPolicy
import com.weeth.domain.notification.domain.service.NotificationTemplateRenderer
import com.weeth.domain.notification.fixture.NotificationIntegrationTestFixture.saveMemberWithCardinals
import com.weeth.domain.notification.fixture.NotificationPreferenceTestFixture
import com.weeth.domain.notification.infrastructure.CodeNotificationTemplateProvider
import com.weeth.domain.notification.infrastructure.JdbcUserNotificationBulkWriterAdapter
import com.weeth.domain.schedule.application.dto.request.ScheduleSaveRequest
import com.weeth.domain.schedule.application.dto.request.ScheduleUpdateRequest
import com.weeth.domain.schedule.application.mapper.EventMapper
import com.weeth.domain.schedule.application.usecase.command.ManageEventUseCase
import com.weeth.domain.schedule.domain.repository.EventRepository
import com.weeth.domain.session.application.dto.request.SessionCreateRequest
import com.weeth.domain.session.application.mapper.SessionMapper
import com.weeth.domain.session.application.usecase.command.CreateSessionUseCase
import com.weeth.domain.session.domain.enums.RecurrenceType
import com.weeth.domain.session.domain.repository.SessionRepository
import com.weeth.domain.session.domain.service.RecurringSessionPolicy
import com.weeth.domain.user.domain.repository.UserRepository
import com.weeth.global.common.id.TsidBase62Encoder
import com.weeth.global.config.TimeConfig
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDateTime

@DataJpaTest(properties = ["notification.dispatch.enabled=true"])
@Import(
    TestContainersConfig::class,
    TimeConfig::class,
    ManageEventUseCase::class,
    CreateSessionUseCase::class,
    ClubPermissionPolicy::class,
    ClubMemberPolicy::class,
    EventMapper::class,
    SessionMapper::class,
    RecurringSessionPolicy::class,
    CreateScheduleNotificationEventListener::class,
    CreateNotificationUseCase::class,
    EnqueueNotificationDeliveryUseCase::class,
    NotificationRecipientPolicy::class,
    NotificationTemplateRenderer::class,
    CodeNotificationTemplateProvider::class,
    JdbcUserNotificationBulkWriterAdapter::class,
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ScheduleNotificationIntegrationTest(
    private val eventUseCase: ManageEventUseCase,
    private val sessionUseCase: CreateSessionUseCase,
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
    private val events: EventRepository,
    private val sessions: SessionRepository,
    private val transactionManager: PlatformTransactionManager,
) : StringSpec({
        data class Audience(
            val clubId: Long,
            val authorId: Long,
            val recipientId: Long,
        )
        val start = LocalDateTime.of(2026, 11, 10, 19, 0)
        val transaction = TransactionTemplate(transactionManager)

        fun audience(): Audience {
            val club = clubs.save(NotificationPreferenceTestFixture.createClub())
            val first = cardinals.save(CardinalTestFixture.createCardinal(club = club, cardinalNumber = 1))
            val other = cardinals.save(CardinalTestFixture.createCardinalInProgress(club = club, cardinalNumber = 2))
            val admin = members.saveMemberWithCardinals(club, users, memberCardinals, first, role = MemberRole.ADMIN)
            val author = admin.user
            var recipientId = 0L
            listOf("eligible", "category-off", "master-off", "other-cardinal", "banned", "inactive").forEach { kind ->
                val member =
                    members.saveMemberWithCardinals(
                        club,
                        users,
                        memberCardinals,
                        if (kind == "other-cardinal") other else first,
                        status = if (kind == "banned") MemberStatus.BANNED else MemberStatus.ACTIVE,
                    )
                val user = member.user
                when (kind) {
                    "eligible" -> {
                        recipientId = user.id
                    }

                    "category-off" -> {
                        preferences.save(
                            NotificationPreference.create(user.id, club.id, NotificationCategory.SCHEDULE, false),
                        )
                    }

                    "master-off" -> {
                        settings.save(
                            NotificationSetting.create(user.id, club.id).apply { updateMaster(false) },
                        )
                    }

                    "inactive" -> {
                        users.save(user.apply { ban() })
                    }
                }
            }
            return Audience(club.id, author.id, recipientId)
        }

        "일정 생성은 해당 기수와 수신 설정을 적용하며 수정·삭제에는 알림을 추가하지 않는다" {
            val audience = audience()
            transaction.executeWithoutResult {
                eventUseCase.create(
                    audience.clubId,
                    ScheduleSaveRequest("개발 모임", cardinal = 1, start = start, end = start.plusHours(2)),
                    audience.authorId,
                )
            }
            val event = events.findAll().single { it.club.id == audience.clubId }
            val notification = notifications.findAll().single { it.clubId == audience.clubId }
            notification.user.id shouldBe audience.recipientId
            notification.type shouldBe NotificationType.EVENT_CREATED
            notification.body shouldContain "개발 모임"
            notification.body shouldContain "2026-11-10 19:00"
            notification.targetPath shouldBe "/clubs/${TsidBase62Encoder.encode(audience.clubId)}/schedules"
            val delivery = deliveries.findAll().single { it.clubId == audience.clubId }
            delivery.eventId shouldBe "EVENT_CREATED:${audience.clubId}:${event.id}"
            outboxes.findAll().count { it.deliveryId == delivery.id } shouldBe 1
            eventUseCase.update(
                audience.clubId,
                event.id,
                ScheduleUpdateRequest("제목 변경", null, null, null, null),
                audience.authorId,
            )
            eventUseCase.delete(audience.clubId, event.id, audience.authorId)
            notifications.findAll().count { it.clubId == audience.clubId } shouldBe 1
        }

        "단일·반복 세션은 생성 요청당 한 알림을 저장하고 반복 그룹으로 식별한다" {
            listOf(false, true).forEach { recurring ->
                val audience = audience()
                transaction.executeWithoutResult {
                    sessionUseCase.create(
                        audience.clubId,
                        SessionCreateRequest(
                            title = "정기 세션",
                            content = null,
                            location = null,
                            cardinal = 1,
                            start = start,
                            end = start.plusHours(2),
                            recurrenceType = if (recurring) RecurrenceType.WEEKLY else null,
                            recurrenceEndDate = if (recurring) start.toLocalDate().plusWeeks(3) else null,
                        ),
                        audience.authorId,
                    )
                }
                val saved = sessions.findAll().filter { it.club.id == audience.clubId }.sortedBy { it.start }
                saved.size shouldBe if (recurring) 4 else 1
                val notification = notifications.findAll().single { it.clubId == audience.clubId }
                notification.user.id shouldBe audience.recipientId
                notification.type shouldBe NotificationType.SESSION_CREATED
                val delivery = deliveries.findAll().single { it.clubId == audience.clubId }
                delivery.body shouldContain "정기 세션"
                delivery.body shouldContain "${saved.size}회"
                delivery.eventId shouldBe
                    if (recurring) {
                        "SESSION_GROUP_CREATED:${audience.clubId}:${saved.first().sessionGroup?.id}"
                    } else {
                        "SESSION_CREATED:${audience.clubId}:${saved.first().id}"
                    }
                outboxes.findAll().count { it.deliveryId == delivery.id } shouldBe 1
            }
        }

        "생성 트랜잭션 롤백은 일정·반복 세션과 알림 작업을 모두 되돌린다" {
            val audience = audience()
            shouldThrow<IllegalStateException> {
                transaction.executeWithoutResult {
                    eventUseCase.create(
                        audience.clubId,
                        ScheduleSaveRequest("취소 일정", cardinal = 1, start = start, end = start.plusHours(1)),
                        audience.authorId,
                    )
                    sessionUseCase.create(
                        audience.clubId,
                        SessionCreateRequest(
                            title = "취소 세션",
                            content = null,
                            location = null,
                            cardinal = 1,
                            start = start,
                            end = start.plusHours(1),
                            recurrenceType = RecurrenceType.WEEKLY,
                            recurrenceEndDate = start.toLocalDate().plusWeeks(2),
                        ),
                        audience.authorId,
                    )
                    error("rollback")
                }
            }
            events.findAll().none { it.club.id == audience.clubId } shouldBe true
            sessions.findAll().none { it.club.id == audience.clubId } shouldBe true
            notifications.findAll().none { it.clubId == audience.clubId } shouldBe true
            deliveries.findAll().none { it.clubId == audience.clubId } shouldBe true
        }
    })
