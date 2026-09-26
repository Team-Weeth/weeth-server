package com.weeth.domain.notification.presentation

import com.weeth.domain.notification.application.dto.request.RegisterNotificationTokenRequest
import com.weeth.domain.notification.application.dto.request.RevokeNotificationTokenRequest
import com.weeth.domain.notification.application.dto.response.NotificationResponse
import com.weeth.domain.notification.application.dto.response.UnreadNotificationCountResponse
import com.weeth.domain.notification.application.usecase.command.ManageNotificationTokenUseCase
import com.weeth.domain.notification.application.usecase.command.ManageNotificationUseCase
import com.weeth.domain.notification.application.usecase.query.GetNotificationQueryService
import com.weeth.domain.notification.domain.enums.NotificationType
import com.weeth.global.common.response.PageResponse
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime

class NotificationControllerTest :
    DescribeSpec({
        val manageNotificationTokenUseCase = mockk<ManageNotificationTokenUseCase>()
        val manageNotificationUseCase = mockk<ManageNotificationUseCase>()
        val getNotificationQueryService = mockk<GetNotificationQueryService>()
        val controller =
            NotificationController(
                manageNotificationTokenUseCase = manageNotificationTokenUseCase,
                manageNotificationUseCase = manageNotificationUseCase,
                getNotificationQueryService = getNotificationQueryService,
            )

        beforeTest {
            clearMocks(
                manageNotificationTokenUseCase,
                manageNotificationUseCase,
                getNotificationQueryService,
            )
        }

        describe("registerToken") {
            it("토큰 등록 성공 코드를 반환한다") {
                val request = RegisterNotificationTokenRequest(token = "fcm-token")
                justRun { manageNotificationTokenUseCase.register(10L, request) }

                val response = controller.registerToken(request, userId = 10L)

                response.code shouldBe NotificationResponseCode.NOTIFICATION_TOKEN_REGISTERED_SUCCESS.code
                response.message shouldBe NotificationResponseCode.NOTIFICATION_TOKEN_REGISTERED_SUCCESS.message
                response.data shouldBe null
                verify(exactly = 1) { manageNotificationTokenUseCase.register(10L, request) }
            }
        }

        describe("revokeToken") {
            it("토큰 해제 성공 코드를 반환한다") {
                val request = RevokeNotificationTokenRequest(token = "fcm-token")
                justRun { manageNotificationTokenUseCase.revoke(10L, request) }

                val response = controller.revokeToken(request, userId = 10L)

                response.code shouldBe NotificationResponseCode.NOTIFICATION_TOKEN_REVOKED_SUCCESS.code
                response.message shouldBe NotificationResponseCode.NOTIFICATION_TOKEN_REVOKED_SUCCESS.message
                response.data shouldBe null
                verify(exactly = 1) { manageNotificationTokenUseCase.revoke(10L, request) }
            }
        }

        describe("countUnread") {
            it("안 읽은 알림 개수 조회 성공 코드를 반환한다") {
                val data = UnreadNotificationCountResponse(count = 3L)
                every { getNotificationQueryService.countUnread(clubId = 1L, userId = 10L) } returns data

                val response = controller.countUnread(clubId = 1L, userId = 10L)

                response.code shouldBe NotificationResponseCode.NOTIFICATION_UNREAD_COUNT_SUCCESS.code
                response.data shouldBe data
            }
        }

        describe("findNotifications") {
            it("알림 목록 조회 성공 코드를 반환한다") {
                val notification =
                    NotificationResponse(
                        id = 1L,
                        type = NotificationType.NOTICE_CREATED,
                        title = "새 공지가 등록되었습니다",
                        body = "중간고사 기간 공지",
                        targetPath = "/clubs/1/boards/10/posts/100",
                        clubId = 1L,
                        boardId = 10L,
                        postId = 100L,
                        isRead = false,
                        createdAt = LocalDateTime.of(2026, 9, 21, 10, 0),
                        readAt = null,
                    )
                val data =
                    PageResponse(
                        content = listOf(notification),
                        pageNumber = 0,
                        pageSize = 20,
                        totalElements = 1,
                        totalPages = 1,
                    )
                every { getNotificationQueryService.findAll(clubId = 1L, userId = 10L, page = 0, size = 20) } returns
                    data

                val response = controller.findNotifications(clubId = 1L, page = 0, size = 20, userId = 10L)

                response.code shouldBe NotificationResponseCode.NOTIFICATION_LIST_SUCCESS.code
                response.data shouldBe data
            }
        }

        describe("markRead") {
            it("알림 읽음 처리 성공 코드를 반환한다") {
                justRun { manageNotificationUseCase.markRead(clubId = 1L, userId = 10L, notificationId = 1L) }

                val response = controller.markRead(clubId = 1L, notificationId = 1L, userId = 10L)

                response.code shouldBe NotificationResponseCode.NOTIFICATION_READ_SUCCESS.code
                verify(
                    exactly = 1,
                ) { manageNotificationUseCase.markRead(clubId = 1L, userId = 10L, notificationId = 1L) }
            }
        }

        describe("markAllRead") {
            it("전체 알림 읽음 처리 성공 코드를 반환한다") {
                justRun { manageNotificationUseCase.markAllRead(clubId = 1L, userId = 10L) }

                val response = controller.markAllRead(clubId = 1L, userId = 10L)

                response.code shouldBe NotificationResponseCode.NOTIFICATION_READ_ALL_SUCCESS.code
                verify(exactly = 1) { manageNotificationUseCase.markAllRead(clubId = 1L, userId = 10L) }
            }
        }
    })
