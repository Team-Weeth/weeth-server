package com.weeth.domain.notification.application.event

import com.weeth.domain.notification.application.usecase.command.ManageNotificationTokenUseCase
import com.weeth.domain.notification.domain.enums.NotificationPlatform
import com.weeth.domain.notification.domain.enums.NotificationReferenceType
import com.weeth.domain.notification.domain.enums.NotificationType
import com.weeth.domain.notification.domain.port.PushNotificationSenderPort
import com.weeth.domain.notification.domain.repository.NotificationTokenReader
import com.weeth.domain.notification.domain.service.NotificationRecipientPolicy
import com.weeth.domain.notification.domain.vo.NotificationContent
import com.weeth.domain.notification.domain.vo.PushNotificationCommand
import com.weeth.domain.notification.domain.vo.PushNotificationResult
import com.weeth.domain.notification.domain.vo.PushTarget
import com.weeth.domain.notification.domain.vo.RenderedNotification
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
        val recipients = mockk<NotificationRecipientPolicy>()
        val listener = SendPushNotificationEventListener(tokenReader, sender, tokenUseCase, recipients)

        beforeTest {
            clearMocks(tokenReader, sender, tokenUseCase, recipients)
            every { recipients.filterEligibleUserIds(any(), any(), any()) } answers { thirdArg() }
        }

        "활성 푸시 대상으로 공통 알림을 발송한다" {
            val target = PushTarget("token", NotificationPlatform.IOS)
            every { tokenReader.findActiveTargetsByUserIds(listOf(2L)) } returns listOf(target)
            every { sender.sendMulticast(any()) } returns PushNotificationResult(listOf("invalid"))

            listener.handle(createEvent())

            verify {
                sender.sendMulticast(
                    match<PushNotificationCommand> {
                        it.targets == listOf(target) && it.data["referenceType"] == "POST" &&
                            it.title == "생성 당시 푸시 제목" && it.body == "생성 당시 푸시 본문"
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

        "생성 후 OFF 또는 탈퇴로 대상이 제외되면 토큰 조회와 실제 발송을 생략한다" {
            every { recipients.filterEligibleUserIds(any(), any(), any()) } returns emptyList()
            listener.handle(createEvent())
            verify(exactly = 0) {
                tokenReader.findActiveTargetsByUserIds(any())
                sender.sendMulticast(any())
            }
        }

        "발송 직전 재검증을 통과한 사용자만 토큰을 조회한다" {
            every { recipients.filterEligibleUserIds(any(), any(), any()) } returns listOf(2L)
            every { tokenReader.findActiveTargetsByUserIds(listOf(2L)) } returns emptyList()
            listener.handle(createEvent().copy(targetUserIds = listOf(2L, 3L)))
            verify(exactly = 1) { tokenReader.findActiveTargetsByUserIds(listOf(2L)) }
        }
    }) {
    private companion object {
        fun createEvent() =
            NotificationCreatedEvent(
                pushMessage = RenderedNotification("생성 당시 푸시 제목", "생성 당시 푸시 본문", 1),
                eventId = "NOTICE_CREATED:62:100",
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
