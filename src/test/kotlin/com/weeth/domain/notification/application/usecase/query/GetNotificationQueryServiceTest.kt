package com.weeth.domain.notification.application.usecase.query

import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.notification.application.mapper.NotificationMapper
import com.weeth.domain.notification.domain.entity.UserNotification
import com.weeth.domain.notification.domain.enums.NotificationType
import com.weeth.domain.notification.domain.repository.UserNotificationReader
import com.weeth.domain.user.fixture.UserTestFixture
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable

class GetNotificationQueryServiceTest :
    DescribeSpec({
        val clubMemberPolicy = mockk<ClubMemberPolicy>()
        val userNotificationReader = mockk<UserNotificationReader>()
        val notificationMapper = NotificationMapper()
        val queryService = GetNotificationQueryService(clubMemberPolicy, userNotificationReader, notificationMapper)

        beforeTest {
            clearMocks(clubMemberPolicy, userNotificationReader)
            every { clubMemberPolicy.getActiveMember(any(), any()) } returns mockk()
        }

        describe("countUnread") {
            it("현재 사용자의 특정 동아리 안 읽은 알림 개수를 응답으로 조립한다") {
                every { userNotificationReader.countByUserIdAndClubIdAndIsReadFalse(10L, 1L) } returns 5L

                val result = queryService.countUnread(clubId = 1L, userId = 10L)

                result.count shouldBe 5L
                verify(exactly = 1) { clubMemberPolicy.getActiveMember(1L, 10L) }
            }
        }

        describe("findAll") {
            it("현재 사용자의 특정 동아리 알림 목록을 최신순 페이지 응답으로 조립한다") {
                val notification = createNotification(userId = 10L)
                val pageable = PageRequest.of(0, 20)
                every {
                    userNotificationReader.findAllByUserIdAndClubIdOrderByCreatedAtDesc(
                        eq(10L),
                        eq(1L),
                        any<Pageable>(),
                    )
                } returns PageImpl(listOf(notification), pageable, 1)

                val result = queryService.findAll(clubId = 1L, userId = 10L, page = 0, size = 20)

                result.content.size shouldBe 1
                result.content.first().title shouldBe "새 공지가 등록되었습니다"
                result.content.first().targetPath shouldBe "/clubs/1/boards/10/posts/100"
                result.pageNumber shouldBe 0
                result.pageSize shouldBe 20
                result.totalElements shouldBe 1L
                verify(exactly = 1) { clubMemberPolicy.getActiveMember(1L, 10L) }
                verify(exactly = 1) {
                    userNotificationReader.findAllByUserIdAndClubIdOrderByCreatedAtDesc(
                        eq(10L),
                        eq(1L),
                        any<Pageable>(),
                    )
                }
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
