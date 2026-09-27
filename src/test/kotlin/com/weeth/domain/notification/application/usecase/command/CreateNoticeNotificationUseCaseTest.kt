package com.weeth.domain.notification.application.usecase.command

import com.weeth.domain.board.application.event.NoticeCreatedEvent
import com.weeth.domain.club.domain.repository.ClubMemberReader
import com.weeth.domain.notification.domain.entity.UserNotification
import com.weeth.domain.notification.domain.enums.NotificationType
import com.weeth.domain.notification.domain.port.UserNotificationBulkWriter
import com.weeth.domain.user.domain.repository.UserReader
import com.weeth.domain.user.fixture.UserTestFixture
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify

class CreateNoticeNotificationUseCaseTest :
    DescribeSpec({
        val clubMemberReader = mockk<ClubMemberReader>()
        val userReader = mockk<UserReader>()
        val userNotificationBulkWriter = mockk<UserNotificationBulkWriter>()
        val useCase = CreateNoticeNotificationUseCase(clubMemberReader, userReader, userNotificationBulkWriter)

        beforeTest {
            clearMocks(clubMemberReader, userReader, userNotificationBulkWriter)
            every { userNotificationBulkWriter.saveAll(any()) } returns Unit
        }

        describe("execute") {
            it("공지 작성자를 제외한 동아리 ACTIVE 멤버별 알림 내역을 저장한다") {
                val event = createEvent(clubId = 62L, authorUserId = 1L)
                val user2 = UserTestFixture.createActiveUser2(2L)
                val user3 = UserTestFixture.createRegisteredUser(3L)
                val notificationsSlot = slot<List<UserNotification>>()

                every {
                    clubMemberReader.findActiveUserIdsByClubIdExcludingUserId(
                        clubId = 62L,
                        cardinalNumber = 7,
                        excludedUserId = 1L,
                    )
                } returns listOf(2L, 3L)
                every { userReader.findAllByIds(listOf(2L, 3L)) } returns listOf(user2, user3)

                useCase.execute(event)

                verify(exactly = 1) {
                    userNotificationBulkWriter.saveAll(capture(notificationsSlot))
                }
                notificationsSlot.captured.map { it.user.id } shouldContainExactlyInAnyOrder listOf(2L, 3L)
                notificationsSlot.captured.forEach {
                    it.type shouldBe NotificationType.NOTICE_CREATED
                    it.title shouldBe "새 공지가 등록되었습니다"
                    it.body shouldBe "중간고사 기간 공지"
                    it.targetPath shouldBe "/clubs/10/boards/10/posts/100"
                    it.clubId shouldBe 62L
                    it.boardId shouldBe 10L
                    it.postId shouldBe 100L
                }
            }

            it("알림 대상 ACTIVE 멤버가 없으면 알림을 저장하지 않는다") {
                every {
                    clubMemberReader.findActiveUserIdsByClubIdExcludingUserId(
                        clubId = 1L,
                        cardinalNumber = 7,
                        excludedUserId = 1L,
                    )
                } returns emptyList()

                useCase.execute(createEvent(authorUserId = 1L))

                verify(exactly = 0) { userReader.findAllByIds(any()) }
                verify(exactly = 0) { userNotificationBulkWriter.saveAll(any()) }
            }
        }
    }) {
    private companion object {
        fun createEvent(
            clubId: Long = 1L,
            authorUserId: Long,
        ): NoticeCreatedEvent =
            NoticeCreatedEvent(
                clubId = clubId,
                boardId = 10L,
                postId = 100L,
                cardinalNumber = 7,
                title = "중간고사 기간 공지",
                authorUserId = authorUserId,
            )
    }
}
