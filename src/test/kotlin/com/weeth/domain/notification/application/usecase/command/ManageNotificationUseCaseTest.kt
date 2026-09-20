package com.weeth.domain.notification.application.usecase.command

import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.notification.application.exception.NotificationNotFoundException
import com.weeth.domain.notification.domain.entity.UserNotification
import com.weeth.domain.notification.domain.enums.NotificationType
import com.weeth.domain.notification.domain.repository.UserNotificationRepository
import com.weeth.domain.user.fixture.UserTestFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class ManageNotificationUseCaseTest :
    DescribeSpec({
        val clubMemberPolicy = mockk<ClubMemberPolicy>()
        val userNotificationRepository = mockk<UserNotificationRepository>()
        val useCase = ManageNotificationUseCase(clubMemberPolicy, userNotificationRepository)

        beforeTest {
            clearMocks(clubMemberPolicy, userNotificationRepository)
            every { clubMemberPolicy.getActiveMember(any(), any()) } returns mockk()
        }

        describe("markRead") {
            it("현재 사용자 소유 알림을 읽음 처리한다") {
                val notification = createNotification(userId = 10L)
                every { userNotificationRepository.findByUserIdAndClubIdAndId(10L, 1L, 100L) } returns notification

                useCase.markRead(clubId = 1L, userId = 10L, notificationId = 100L)

                notification.isRead shouldBe true
                verify(exactly = 1) { clubMemberPolicy.getActiveMember(1L, 10L) }
                verify(exactly = 1) { userNotificationRepository.findByUserIdAndClubIdAndId(10L, 1L, 100L) }
            }

            it("현재 사용자 소유 알림이 없으면 예외를 던진다") {
                every { userNotificationRepository.findByUserIdAndClubIdAndId(10L, 1L, 999L) } returns null

                shouldThrow<NotificationNotFoundException> {
                    useCase.markRead(clubId = 1L, userId = 10L, notificationId = 999L)
                }
            }
        }

        describe("markAllRead") {
            it("현재 사용자의 특정 동아리 안 읽은 알림을 모두 읽음 처리한다") {
                every { userNotificationRepository.markAllReadByUserIdAndClubId(eq(10L), eq(1L), any()) } returns 3

                useCase.markAllRead(clubId = 1L, userId = 10L)

                verify(exactly = 1) { clubMemberPolicy.getActiveMember(1L, 10L) }
                verify(exactly = 1) { userNotificationRepository.markAllReadByUserIdAndClubId(eq(10L), eq(1L), any()) }
            }
        }
    }) {
    private companion object {
        fun createNotification(userId: Long): UserNotification =
            UserNotification.create(
                user = UserTestFixture.createActiveUser1(userId),
                type = NotificationType.NOTICE_CREATED,
                title = "새 공지가 등록되었습니다",
                body = "중간고사 기간 공지",
                targetPath = "/clubs/1/boards/10/posts/100",
                clubId = 1L,
                boardId = 10L,
                postId = 100L,
            )
    }
}
