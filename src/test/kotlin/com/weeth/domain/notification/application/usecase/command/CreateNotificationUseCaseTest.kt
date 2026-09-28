package com.weeth.domain.notification.application.usecase.command

import com.weeth.domain.notification.application.event.NotificationCreatedEvent
import com.weeth.domain.notification.domain.entity.UserNotification
import com.weeth.domain.notification.domain.enums.NotificationReferenceType
import com.weeth.domain.notification.domain.enums.NotificationType
import com.weeth.domain.notification.domain.port.UserNotificationBulkWriter
import com.weeth.domain.notification.domain.vo.NotificationContent
import com.weeth.domain.user.domain.repository.UserReader
import com.weeth.domain.user.fixture.UserTestFixture
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.springframework.context.ApplicationEventPublisher

class CreateNotificationUseCaseTest :
    StringSpec({
        val userReader = mockk<UserReader>()
        val bulkWriter = mockk<UserNotificationBulkWriter>()
        val eventPublisher = mockk<ApplicationEventPublisher>()
        val useCase = CreateNotificationUseCase(userReader, bulkWriter, eventPublisher)

        beforeTest {
            clearMocks(userReader, bulkWriter, eventPublisher)
        }

        "알림을 저장하고 공통 생성 이벤트를 발행한다" {
            val content = createContent("가".repeat(300))
            val notificationSlot = slot<List<UserNotification>>()
            val eventSlot = slot<NotificationCreatedEvent>()
            every { userReader.findAllByIds(listOf(2L)) } returns listOf(UserTestFixture.createActiveUser2(2L))
            every { bulkWriter.saveAll(capture(notificationSlot)) } returns Unit
            every { eventPublisher.publishEvent(capture(eventSlot)) } returns Unit

            useCase.execute(content, listOf(2L))

            notificationSlot.captured.single().referenceType shouldBe NotificationReferenceType.POST
            notificationSlot.captured.single().referenceId shouldBe 100L
            notificationSlot.captured
                .single()
                .body.length shouldBe 255
            notificationSlot.captured
                .single()
                .body
                .last() shouldBe '…'
            eventSlot.captured.content.body shouldBe notificationSlot.captured.single().body
            eventSlot.captured.targetUserIds shouldBe listOf(2L)
        }

        "대상이 없으면 조회와 저장을 생략한다" {
            useCase.execute(createContent("공지"), emptyList())

            verify(exactly = 0) { userReader.findAllByIds(any()) }
            verify(exactly = 0) { bulkWriter.saveAll(any()) }
            verify(exactly = 0) { eventPublisher.publishEvent(any<Any>()) }
        }

        "실제로 조회된 사용자만 푸시 발송 대상으로 전달한다" {
            val eventSlot = slot<NotificationCreatedEvent>()
            every { userReader.findAllByIds(listOf(2L, 3L)) } returns
                listOf(UserTestFixture.createActiveUser2(2L))
            every { bulkWriter.saveAll(any()) } returns Unit
            every { eventPublisher.publishEvent(capture(eventSlot)) } returns Unit

            useCase.execute(createContent("공지"), listOf(2L, 3L))

            eventSlot.captured.targetUserIds shouldBe listOf(2L)
        }
    }) {
    private companion object {
        fun createContent(body: String) =
            NotificationContent.create(
                type = NotificationType.NOTICE_CREATED,
                title = "새 공지가 등록되었습니다",
                body = body,
                targetPath = "/clubs/10/boards/10/posts/100",
                clubId = 62L,
                referenceType = NotificationReferenceType.POST,
                referenceId = 100L,
                data = emptyMap(),
            )
    }
}
