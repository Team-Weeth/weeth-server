package com.weeth.domain.notification.domain.repository

import com.weeth.config.TestContainersConfig
import com.weeth.domain.notification.domain.entity.NotificationToken
import com.weeth.domain.notification.domain.entity.UserNotification
import com.weeth.domain.notification.domain.enums.NotificationType
import com.weeth.domain.user.domain.repository.UserRepository
import com.weeth.domain.user.fixture.UserTestFixture
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import java.time.LocalDateTime

@DataJpaTest
@Import(TestContainersConfig::class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class NotificationRepositoryTest(
    private val notificationTokenRepository: NotificationTokenRepository,
    private val userNotificationRepository: UserNotificationRepository,
    private val userRepository: UserRepository,
) : StringSpec({

        "findActiveTokensByUserIds는 요청 사용자들의 활성 토큰 값만 조회한다" {
            val user1 = userRepository.save(UserTestFixture.createActiveUser1())
            val user2 = userRepository.save(UserTestFixture.createActiveUser2())
            val excludedUser = userRepository.save(UserTestFixture.createRegisteredUser())
            val registeredAt = LocalDateTime.of(2026, 9, 21, 10, 0)
            val inactiveToken = NotificationToken.create(user1, "inactive-token", registeredAt)

            inactiveToken.deactivate()
            notificationTokenRepository.saveAll(
                listOf(
                    NotificationToken.create(user1, "active-token-1", registeredAt),
                    inactiveToken,
                    NotificationToken.create(user2, "active-token-2", registeredAt),
                    NotificationToken.create(excludedUser, "excluded-token", registeredAt),
                ),
            )

            val result = notificationTokenRepository.findActiveTokensByUserIds(listOf(user1.id, user2.id))

            result shouldContainExactlyInAnyOrder listOf("active-token-1", "active-token-2")
        }

        "deactivateInvalidTokens는 발송 시작 시각 이전에 등록된 토큰만 비활성화한다" {
            val user = userRepository.save(UserTestFixture.createActiveUser1())
            val sendStartedAt = LocalDateTime.of(2026, 9, 21, 10, 0)
            val oldToken = NotificationToken.create(user, "old-invalid-token", sendStartedAt.minusMinutes(1))
            val newToken = NotificationToken.create(user, "newly-registered-token", sendStartedAt.plusMinutes(1))

            notificationTokenRepository.saveAll(listOf(oldToken, newToken))

            val updatedCount =
                notificationTokenRepository.deactivateInvalidTokens(
                    invalidTokens = listOf(oldToken.token, newToken.token),
                    registeredBeforeOrAt = sendStartedAt,
                )

            updatedCount shouldBe 1
            notificationTokenRepository.findByToken("old-invalid-token")?.isActive shouldBe false
            notificationTokenRepository.findByToken("newly-registered-token")?.isActive shouldBe true
        }

        "findTargetUserIdsByTypeAndPostId는 저장된 공지 알림 대상 사용자 id만 조회한다" {
            val user1 = userRepository.save(UserTestFixture.createActiveUser1())
            val user2 = userRepository.save(UserTestFixture.createActiveUser2())
            val otherUser = userRepository.save(UserTestFixture.createRegisteredUser())

            userNotificationRepository.saveAll(
                listOf(
                    createNoticeNotification(user1, postId = 100L),
                    createNoticeNotification(user2, postId = 100L),
                    createNoticeNotification(otherUser, postId = 200L),
                ),
            )

            val result =
                userNotificationRepository.findTargetUserIdsByTypeAndPostId(
                    type = NotificationType.NOTICE_CREATED,
                    postId = 100L,
                )

            result shouldContainExactlyInAnyOrder listOf(user1.id, user2.id)
        }
    }) {
    private companion object {
        fun createNoticeNotification(
            user: com.weeth.domain.user.domain.entity.User,
            postId: Long,
        ): UserNotification =
            UserNotification.create(
                user = user,
                type = NotificationType.NOTICE_CREATED,
                title = "새 공지가 등록되었습니다",
                body = "중간고사 기간 공지",
                targetPath = "/clubs/1/boards/10/posts/$postId",
                clubId = 1L,
                boardId = 10L,
                postId = postId,
            )
    }
}
