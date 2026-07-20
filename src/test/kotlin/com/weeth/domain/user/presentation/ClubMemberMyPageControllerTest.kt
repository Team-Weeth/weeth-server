package com.weeth.domain.user.presentation

import com.weeth.domain.attendance.domain.enums.AttendanceStatus
import com.weeth.domain.user.application.dto.response.UserAttendedSessionResponse
import com.weeth.domain.user.application.dto.response.UserMyPostResponse
import com.weeth.domain.user.application.usecase.query.GetUserAttendanceQueryService
import com.weeth.domain.user.application.usecase.query.GetUserPostQueryService
import com.weeth.global.common.response.SliceResponse
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime

class ClubMemberMyPageControllerTest :
    DescribeSpec({
        val getUserPostQueryService = mockk<GetUserPostQueryService>()
        val getUserAttendanceQueryService = mockk<GetUserAttendanceQueryService>()
        val controller =
            ClubMemberMyPageController(
                getUserPostQueryService = getUserPostQueryService,
                getUserAttendanceQueryService = getUserAttendanceQueryService,
            )

        beforeTest {
            clearMocks(getUserPostQueryService, getUserAttendanceQueryService)
        }

        describe("getMyPosts") {
            it("현재 동아리에서 로그인 사용자가 작성한 게시글 목록을 조회한다") {
                val responseBody =
                    SliceResponse(
                        content =
                            listOf(
                                UserMyPostResponse(
                                    postId = 200L,
                                    clubId = "1C",
                                    clubName = "Leets",
                                    boardId = 10L,
                                    boardName = "자유게시판",
                                    title = "제목",
                                    content = "내용",
                                    commentCount = 3,
                                    likeCount = 5,
                                    createdAt = LocalDateTime.of(2026, 6, 29, 10, 0),
                                    isNew = true,
                                ),
                            ),
                        pageNumber = 0,
                        pageSize = 5,
                        numberOfElements = 1,
                        hasNext = true,
                    )
                every { getUserPostQueryService.getMyPosts(1L, 100L, 0, 5) } returns responseBody

                val response = controller.getMyPosts(clubId = 100L, userId = 1L, pageNumber = 0, pageSize = 5)

                response.code shouldBe UserResponseCode.USER_MY_POSTS_FIND_SUCCESS.code
                response.message shouldBe UserResponseCode.USER_MY_POSTS_FIND_SUCCESS.message
                response.data shouldBe responseBody
                verify(exactly = 1) { getUserPostQueryService.getMyPosts(1L, 100L, 0, 5) }
            }
        }

        describe("getAttendedSessions") {
            it("현재 동아리에서 로그인 사용자가 출석한 세션 목록을 조회한다") {
                val responseBody =
                    SliceResponse(
                        content =
                            listOf(
                                UserAttendedSessionResponse(
                                    attendanceId = 1L,
                                    clubId = "1C",
                                    clubName = "Leets",
                                    sessionId = 10L,
                                    sessionTitle = "1차 정기모임",
                                    cardinal = 6,
                                    start = LocalDateTime.of(2026, 6, 29, 19, 0),
                                    end = LocalDateTime.of(2026, 6, 29, 21, 0),
                                    status = AttendanceStatus.ATTEND,
                                ),
                            ),
                        pageNumber = 0,
                        pageSize = 5,
                        numberOfElements = 1,
                        hasNext = false,
                    )
                every { getUserAttendanceQueryService.getAttendedSessions(1L, 100L, 0, 5) } returns responseBody

                val response = controller.getAttendedSessions(clubId = 100L, userId = 1L, pageNumber = 0, pageSize = 5)

                response.code shouldBe UserResponseCode.USER_ATTENDED_SESSIONS_FIND_SUCCESS.code
                response.message shouldBe UserResponseCode.USER_ATTENDED_SESSIONS_FIND_SUCCESS.message
                response.data shouldBe responseBody
                verify(exactly = 1) { getUserAttendanceQueryService.getAttendedSessions(1L, 100L, 0, 5) }
            }
        }
    })
