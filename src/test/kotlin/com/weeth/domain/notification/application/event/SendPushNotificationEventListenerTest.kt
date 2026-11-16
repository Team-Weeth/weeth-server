package com.weeth.domain.notification.application.event

import com.weeth.domain.notification.application.usecase.command.ManageNotificationTokenUseCase
import com.weeth.domain.notification.domain.enums.NotificationPlatform
import com.weeth.domain.notification.domain.enums.NotificationReferenceType
import com.weeth.domain.notification.domain.enums.NotificationType
import com.weeth.domain.notification.domain.port.PushNotificationSenderPort
import com.weeth.domain.notification.domain.repository.NotificationTokenReader
import com.weeth.domain.notification.domain.vo.NotificationContent
import com.weeth.domain.notification.domain.vo.PushNotificationCommand
import com.weeth.domain.notification.domain.vo.PushNotificationResult
import com.weeth.domain.notification.domain.vo.PushTarget
import io.kotest.core.spec.style.StringSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class SendPushNotificationEventListenerTest :
    StringSpec({
        val tokenReader = mockk<NotificationTokenReader>()
        val sender = mockk<PushNotificationSenderPort>()
        val tokenUseCase = mockk<ManageNotificationTokenUseCase>(relaxed = true)
        val listener = SendPushNotificationEventListener(tokenReader, sender, tokenUseCase)

        beforeTest {
            clearMocks(tokenReader, sender, tokenUseCase)
        }

        "활성 푸시 대상으로 공통 알림을 발송한다" {
            val target = PushTarget("token", NotificationPlatform.IOS)
            every { tokenReader.findActiveTargetsByUserIds(listOf(2L)) } returns listOf(target)
            every { sender.sendMulticast(any()) } returns PushNotificationResult(listOf("invalid"))

            listener.handle(createEvent())

            verify {
                sender.sendMulticast(
                    match<PushNotificationCommand> {
                        it.targets == listOf(target) && it.data["referenceType"] == "POST"
                    },
                )
            }
            verify { tokenUseCase.deactivateInvalidTokens(eq(listOf("invalid")), any()) }
        }

        "활성 푸시 대상이 없으면 발송하지 않는다" {
            every { tokenReader.findActiveTargetsByUserIds(listOf(2L)) } returns emptyList()

            listener.handle(createEvent())

            verify(exactly = 0) { sender.sendMulticast(any()) }
        }
    }) {
    private companion object {
        fun createEvent() =
            NotificationCreatedEvent(
                content =
                    NotificationContent.create(
                        type = NotificationType.NOTICE_CREATED,
                        title = "새 공지가 등록되었습니다",
                        body = "공지",
                        targetPath = "/clubs/10/boards/10/posts/100",
                        clubId = 62L,
                        referenceType = NotificationReferenceType.POST,
                        referenceId = 100L,
                        data = mapOf("referenceType" to "POST"),
                    ),
                targetUserIds = listOf(2L),
            )
    }
}
