package com.weeth.domain.notification.application.event

import com.weeth.domain.board.application.event.NoticeCreatedEvent
import com.weeth.domain.notification.application.usecase.command.ManageNotificationTokenUseCase
import com.weeth.domain.notification.domain.enums.NotificationType
import com.weeth.domain.notification.domain.port.PushNotificationSenderPort
import com.weeth.domain.notification.domain.repository.NotificationTokenReader
import com.weeth.domain.notification.domain.repository.UserNotificationReader
import com.weeth.domain.notification.domain.vo.PushNotificationCommand
import com.weeth.domain.notification.domain.vo.PushNotificationResult
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import java.time.LocalDateTime

class SendNoticePushEventListenerTest :
    DescribeSpec({
        val userNotificationReader = mockk<UserNotificationReader>()
        val notificationTokenReader = mockk<NotificationTokenReader>()
        val pushNotificationSenderPort = mockk<PushNotificationSenderPort>()
        val manageNotificationTokenUseCase = mockk<ManageNotificationTokenUseCase>()
        val listener =
            SendNoticePushEventListener(
                userNotificationReader,
                notificationTokenReader,
                pushNotificationSenderPort,
                manageNotificationTokenUseCase,
            )

        beforeTest {
            clearMocks(
                userNotificationReader,
                notificationTokenReader,
                pushNotificationSenderPort,
                manageNotificationTokenUseCase,
            )
            every { manageNotificationTokenUseCase.deactivateInvalidTokens(any(), any()) } just runs
        }

        describe("handle") {
            it("저장된 공지 알림 대상자의 활성 토큰으로 푸시를 발송하고 invalid token을 비활성화한다") {
                val event = createEvent()
                every {
                    userNotificationReader.findTargetUserIdsByTypeAndPostId(NotificationType.NOTICE_CREATED, 100L)
                } returns listOf(2L, 3L)
                every { notificationTokenReader.findActiveTokensByUserIds(listOf(2L, 3L)) } returns
                    listOf("token-2", "token-3")
                every { pushNotificationSenderPort.sendMulticast(any()) } returns
                    PushNotificationResult(invalidTokens = listOf("token-3"))

                listener.handle(event)

                verify(exactly = 1) {
                    pushNotificationSenderPort.sendMulticast(
                        match<PushNotificationCommand> {
                            it.title == "새 공지가 등록되었습니다" &&
                                it.body == "중간고사 기간 공지" &&
                                it.tokens == listOf("token-2", "token-3") &&
                                it.data["type"] == "NOTICE_CREATED" &&
                                it.data["clubId"] == "1" &&
                                it.data["boardId"] == "10" &&
                                it.data["postId"] == "100" &&
                                it.data["targetPath"] == "/clubs/1/boards/10/posts/100"
                        },
                    )
                }
                verify(exactly = 1) {
                    manageNotificationTokenUseCase.deactivateInvalidTokens(
                        invalidTokens = listOf("token-3"),
                        registeredBeforeOrAt = any<LocalDateTime>(),
                    )
                }
            }

            it("활성 토큰이 없으면 푸시를 발송하지 않는다") {
                val event = createEvent()
                every {
                    userNotificationReader.findTargetUserIdsByTypeAndPostId(NotificationType.NOTICE_CREATED, 100L)
                } returns listOf(2L)
                every { notificationTokenReader.findActiveTokensByUserIds(listOf(2L)) } returns emptyList()

                listener.handle(event)

                verify(exactly = 0) { pushNotificationSenderPort.sendMulticast(any()) }
                verify(exactly = 0) { manageNotificationTokenUseCase.deactivateInvalidTokens(any(), any()) }
            }

            it("푸시 발송이 실패해도 예외를 전파하지 않는다") {
                val event = createEvent()
                every {
                    userNotificationReader.findTargetUserIdsByTypeAndPostId(NotificationType.NOTICE_CREATED, 100L)
                } returns listOf(2L)
                every { notificationTokenReader.findActiveTokensByUserIds(listOf(2L)) } returns listOf("token-2")
                every { pushNotificationSenderPort.sendMulticast(any()) } throws RuntimeException("fcm failed")

                shouldNotThrowAny {
                    listener.handle(event)
                }

                verify(exactly = 0) { manageNotificationTokenUseCase.deactivateInvalidTokens(any(), any()) }
            }
        }
    }) {
    private companion object {
        fun createEvent(): NoticeCreatedEvent =
            NoticeCreatedEvent(
                clubId = 1L,
                boardId = 10L,
                postId = 100L,
                title = "중간고사 기간 공지",
                authorUserId = 1L,
            )
    }
}
