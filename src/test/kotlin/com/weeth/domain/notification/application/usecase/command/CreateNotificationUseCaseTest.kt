package com.weeth.domain.notification.application.usecase.command

import com.weeth.domain.notification.application.event.NotificationCreatedEvent
import com.weeth.domain.notification.domain.entity.NotificationFailure
import com.weeth.domain.notification.domain.entity.UserNotification
import com.weeth.domain.notification.domain.enums.NotificationChannel
import com.weeth.domain.notification.domain.enums.NotificationTemplateFailureReason
import com.weeth.domain.notification.domain.port.NotificationTemplateProvider
import com.weeth.domain.notification.domain.port.UserNotificationBulkWriter
import com.weeth.domain.notification.domain.repository.NotificationFailureRepository
import com.weeth.domain.notification.domain.service.NotificationRecipientPolicy
import com.weeth.domain.notification.domain.service.NotificationTemplateRenderer
import com.weeth.domain.notification.domain.vo.NotificationTemplate
import com.weeth.domain.notification.fixture.NotificationRequestTestFixture
import com.weeth.domain.user.domain.repository.UserReader
import com.weeth.domain.user.fixture.UserTestFixture
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.springframework.context.ApplicationEventPublisher

class CreateNotificationUseCaseTest :
    DescribeSpec({
        val users = mockk<UserReader>()
        val writer = mockk<UserNotificationBulkWriter>()
        val publisher = mockk<ApplicationEventPublisher>()
        val recipients = mockk<NotificationRecipientPolicy>()
        val templates = mockk<NotificationTemplateProvider>()
        val failures = mockk<NotificationFailureRepository>()
        val useCase =
            CreateNotificationUseCase(
                users,
                writer,
                publisher,
                recipients,
                NotificationTemplateRenderer(templates),
                failures,
            )

        beforeTest {
            clearMocks(users, writer, publisher, recipients, templates, failures)
            every { recipients.filterEligibleUserIds(any(), any(), any()) } answers { thirdArg() }
            every { templates.find(any(), any(), any()) } answers {
                val push = secondArg<NotificationChannel>() == NotificationChannel.PUSH
                NotificationTemplate(
                    if (push) 2 else 1,
                    if (push) "푸시 제목" else "앱 내 제목",
                    "{noticeTitle}",
                    setOf("noticeTitle"),
                )
            }
            every { users.findAllByIds(any()) } answers {
                firstArg<List<Long>>().map { UserTestFixture.createActiveUser1(it) }
            }
            every { writer.saveAll(any()) } returns Unit
            every { publisher.publishEvent(any<Any>()) } returns Unit
            every { failures.save(any()) } answers { firstArg() }
        }

        describe("템플릿 기반 공지 생성") {
            it("채널별 제목·본문·버전을 분리하고 앱 내 알림과 푸시 snapshot을 생성한다") {
                val notifications = slot<List<UserNotification>>()
                val event = slot<NotificationCreatedEvent>()
                every { writer.saveAll(capture(notifications)) } returns Unit
                every { publisher.publishEvent(capture(event)) } returns Unit
                useCase.execute(NotificationRequestTestFixture.createNotice(title = "가".repeat(300)), listOf(2L))
                notifications.captured.single().title shouldBe "앱 내 제목"
                notifications.captured
                    .single()
                    .body.length shouldBe 255
                notifications.captured
                    .single()
                    .body
                    .last() shouldBe '…'
                notifications.captured.single().templateVersion shouldBe 1
                event.captured.pushMessage.title shouldBe "푸시 제목"
                event.captured.pushMessage.templateVersion shouldBe 2
                event.captured.targetUserIds shouldBe listOf(2L)
            }

            it("후보가 없거나 모두 수신 거부이면 저장·렌더링·이벤트 발행을 하지 않는다") {
                useCase.execute(NotificationRequestTestFixture.createNotice(), emptyList())
                every { recipients.filterEligibleUserIds(any(), any(), any()) } returns emptyList()
                useCase.execute(NotificationRequestTestFixture.createNotice(), listOf(2L))
                verify(exactly = 0) {
                    users.findAllByIds(any())
                    writer.saveAll(any())
                    templates.find(any(), any(), any())
                    publisher.publishEvent(any<Any>())
                }
            }

            it("허용된 사용자 중 실제 조회된 사용자만 저장·발송한다") {
                every { recipients.filterEligibleUserIds(any(), any(), any()) } returns listOf(2L, 3L)
                every { users.findAllByIds(listOf(2L, 3L)) } returns listOf(UserTestFixture.createActiveUser1(2L))
                val event = slot<NotificationCreatedEvent>()
                every { publisher.publishEvent(capture(event)) } returns Unit
                useCase.execute(NotificationRequestTestFixture.createNotice(), listOf(2L, 3L, 4L))
                event.captured.targetUserIds shouldBe listOf(2L)
            }

            it("필수 변수 누락은 비즈니스 예외 없이 실패 이력만 저장한다") {
                val failure = slot<NotificationFailure>()
                every { failures.save(capture(failure)) } answers { firstArg() }
                useCase.execute(NotificationRequestTestFixture.createNotice(title = null), listOf(2L))
                failure.captured.eventId shouldBe "NOTICE_CREATED:62:100"
                failure.captured.reason shouldBe NotificationTemplateFailureReason.MISSING_VARIABLES
                failure.captured.missingVariables shouldBe "noticeTitle"
                failure.captured.channel shouldBe NotificationChannel.IN_APP
                verify(exactly = 0) {
                    writer.saveAll(any())
                    publisher.publishEvent(any<Any>())
                }
            }

            it("PUSH 템플릿이 없으면 앱 내 알림도 저장하지 않고 PUSH 실패를 기록한다") {
                every { templates.find(any(), NotificationChannel.PUSH, any()) } returns null
                val failure = slot<NotificationFailure>()
                every { failures.save(capture(failure)) } answers { firstArg() }
                useCase.execute(NotificationRequestTestFixture.createNotice(), listOf(2L))
                failure.captured.channel shouldBe NotificationChannel.PUSH
                failure.captured.reason shouldBe NotificationTemplateFailureReason.TEMPLATE_NOT_FOUND
                verify(exactly = 0) {
                    writer.saveAll(any())
                    publisher.publishEvent(any<Any>())
                }
            }
        }
    })
