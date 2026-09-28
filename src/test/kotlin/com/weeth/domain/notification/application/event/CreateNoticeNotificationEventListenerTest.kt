package com.weeth.domain.notification.application.event

import com.weeth.domain.board.application.event.NoticeCreatedEvent
import com.weeth.domain.club.domain.repository.ClubMemberReader
import com.weeth.domain.notification.application.usecase.command.CreateNotificationUseCase
import com.weeth.domain.notification.domain.enums.NotificationReferenceType
import com.weeth.domain.notification.domain.vo.NotificationContent
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify

class CreateNoticeNotificationEventListenerTest :
    StringSpec({
        val clubMemberReader = mockk<ClubMemberReader>()
        val createNotificationUseCase = mockk<CreateNotificationUseCase>(relaxed = true)
        val listener = CreateNoticeNotificationEventListener(clubMemberReader, createNotificationUseCase)

        "공지 이벤트를 공통 알림 내용으로 변환한다" {
            val contentSlot = slot<NotificationContent>()
            every {
                clubMemberReader.findActiveUserIdsByClubIdExcludingUserId(62L, 7, 1L)
            } returns listOf(2L, 3L)

            listener.handle(
                NoticeCreatedEvent(
                    clubId = 62L,
                    boardId = 10L,
                    postId = 100L,
                    cardinalNumber = 7,
                    title = "중간고사 기간 공지",
                    authorUserId = 1L,
                ),
            )

            verify { createNotificationUseCase.execute(capture(contentSlot), listOf(2L, 3L)) }
            contentSlot.captured.referenceType shouldBe NotificationReferenceType.POST
            contentSlot.captured.referenceId shouldBe 100L
            contentSlot.captured.targetPath shouldBe "/clubs/10/boards/10/posts/100"
            contentSlot.captured.data["clubId"] shouldBe "10"
        }
    })
