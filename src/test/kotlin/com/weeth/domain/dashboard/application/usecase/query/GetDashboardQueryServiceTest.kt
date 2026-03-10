package com.weeth.domain.dashboard.application.usecase.query

import com.weeth.domain.board.domain.enums.BoardType
import com.weeth.domain.board.domain.repository.NoticeReadReader
import com.weeth.domain.board.domain.repository.PostReader
import com.weeth.domain.board.fixture.BoardTestFixture
import com.weeth.domain.board.fixture.PostTestFixture
import com.weeth.domain.club.domain.repository.ClubMemberReader
import com.weeth.domain.club.domain.repository.ClubReader
import com.weeth.domain.club.fixture.ClubTestFixture
import com.weeth.domain.dashboard.application.exception.DashboardNotClubMemberException
import com.weeth.domain.dashboard.application.mapper.DashboardMapper
import com.weeth.domain.file.application.mapper.FileMapper
import com.weeth.domain.file.domain.enums.FileOwnerType
import com.weeth.domain.file.domain.repository.FileReader
import com.weeth.domain.schedule.domain.repository.EventReader
import com.weeth.domain.schedule.fixture.ScheduleTestFixture
import com.weeth.domain.session.domain.repository.SessionReader
import com.weeth.domain.session.fixture.SessionTestFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.SliceImpl
import java.time.LocalDateTime

class GetDashboardQueryServiceTest :
    DescribeSpec({
        val clubReader = mockk<ClubReader>()
        val clubMemberReader = mockk<ClubMemberReader>()
        val eventReader = mockk<EventReader>()
        val sessionReader = mockk<SessionReader>()
        val postReader = mockk<PostReader>()
        val noticeReadReader = mockk<NoticeReadReader>()
        val fileReader = mockk<FileReader>()
        val fileMapper = mockk<FileMapper>()
        val dashboardMapper = DashboardMapper(fileMapper)

        val queryService =
            GetDashboardQueryService(
                clubReader = clubReader,
                clubMemberReader = clubMemberReader,
                eventReader = eventReader,
                sessionReader = sessionReader,
                postReader = postReader,
                noticeReadReader = noticeReadReader,
                fileReader = fileReader,
                dashboardMapper = dashboardMapper,
            )

        val clubId = 1L
        val userId = 1L
        val club = ClubTestFixture.createClub()
        val clubMember = ClubTestFixture.createClubMember(club = club)

        beforeTest {
            clearMocks(
                clubReader,
                clubMemberReader,
                eventReader,
                sessionReader,
                postReader,
                noticeReadReader,
                fileReader,
                fileMapper,
            )
        }

        describe("getHome") {
            context("활성 멤버인 경우") {
                it("홈 정보를 반환한다") {
                    every { clubReader.getClubById(clubId) } returns club
                    every { clubMemberReader.findByClubIdAndUserId(clubId, userId) } returns clubMember
                    every { clubMemberReader.countActiveByClubId(clubId) } returns 10L
                    every { eventReader.findByDateRange(any(), any()) } returns emptyList()
                    every {
                        sessionReader.findByStartLessThanEqualAndEndGreaterThanEqualOrderByStartAsc(any(), any())
                    } returns emptyList()
                    every { clubMemberReader.findActiveByUserId(userId) } returns listOf(clubMember)

                    val result = queryService.getHome(clubId, userId)

                    result shouldNotBe null
                    result.club.memberCount shouldBe 10L
                    result.myClubs.size shouldBe 1
                }
            }

            context("멤버가 아닌 경우") {
                it("DashboardNotClubMemberException을 던진다") {
                    every { clubReader.getClubById(clubId) } returns club
                    every { clubMemberReader.findByClubIdAndUserId(clubId, userId) } returns null

                    shouldThrow<DashboardNotClubMemberException> {
                        queryService.getHome(clubId, userId)
                    }
                }
            }

            context("오늘 일정이 있는 경우") {
                it("이벤트와 세션을 시작 시간순으로 정렬하여 반환한다") {
                    val event =
                        ScheduleTestFixture.createEvent(
                            id = 1L,
                            start = LocalDateTime.now().withHour(10).withMinute(0),
                            end = LocalDateTime.now().withHour(12).withMinute(0),
                        )
                    val session =
                        SessionTestFixture.createSession(
                            id = 2L,
                            start = LocalDateTime.now().withHour(14).withMinute(0),
                            end = LocalDateTime.now().withHour(16).withMinute(0),
                        )

                    every { clubReader.getClubById(clubId) } returns club
                    every { clubMemberReader.findByClubIdAndUserId(clubId, userId) } returns clubMember
                    every { clubMemberReader.countActiveByClubId(clubId) } returns 5L
                    every { eventReader.findByDateRange(any(), any()) } returns listOf(event)
                    every {
                        sessionReader.findByStartLessThanEqualAndEndGreaterThanEqualOrderByStartAsc(any(), any())
                    } returns listOf(session)
                    every { clubMemberReader.findActiveByUserId(userId) } returns listOf(clubMember)

                    val result = queryService.getHome(clubId, userId)

                    result.todaySchedules.size shouldBe 2
                    result.todaySchedules[0].isMeeting shouldBe false
                    result.todaySchedules[1].isMeeting shouldBe true
                }
            }
        }

        describe("getRecentPosts") {
            context("멤버인 경우") {
                it("공지 제외한 최신 게시글을 반환한다") {
                    val board = BoardTestFixture.create(type = BoardType.GENERAL)
                    val post = PostTestFixture.create(board = board)
                    val pageable = PageRequest.of(0, 10)
                    val slice = SliceImpl(listOf(post), pageable, false)

                    every { clubMemberReader.findByClubIdAndUserId(clubId, userId) } returns clubMember
                    every { postReader.findRecentExcludingBoardType(BoardType.NOTICE, any()) } returns slice
                    every { fileReader.findAll(FileOwnerType.POST, any<List<Long>>()) } returns emptyList()

                    val result = queryService.getRecentPosts(clubId, userId, 0, 10)

                    result.content.size shouldBe 1
                    result.content[0].fileUrls.isEmpty() shouldBe true
                }
            }

            context("멤버가 아닌 경우") {
                it("DashboardNotClubMemberException을 던진다") {
                    every { clubMemberReader.findByClubIdAndUserId(clubId, userId) } returns null

                    shouldThrow<DashboardNotClubMemberException> {
                        queryService.getRecentPosts(clubId, userId, 0, 10)
                    }
                }
            }
        }

        describe("getRecentNotices") {
            context("멤버인 경우") {
                it("최신 공지 목록을 반환한다") {
                    val noticeBoard = BoardTestFixture.create(type = BoardType.NOTICE)
                    val notice = PostTestFixture.create(board = noticeBoard)
                    val pageable = PageRequest.of(0, 5)
                    val slice = SliceImpl(listOf(notice), pageable, false)

                    every { clubMemberReader.findByClubIdAndUserId(clubId, userId) } returns clubMember
                    every { postReader.findRecentByBoardType(BoardType.NOTICE, any()) } returns slice

                    val result = queryService.getRecentNotices(clubId, userId)

                    result.size shouldBe 1
                }
            }
        }

        describe("getMonthlySchedules") {
            context("멤버인 경우") {
                it("월간 일정 목록을 시작 시간순으로 반환한다") {
                    val event = ScheduleTestFixture.createEvent(id = 1L)

                    every { clubMemberReader.findByClubIdAndUserId(clubId, userId) } returns clubMember
                    every { eventReader.findByDateRange(any(), any()) } returns listOf(event)
                    every {
                        sessionReader.findByStartLessThanEqualAndEndGreaterThanEqualOrderByStartAsc(any(), any())
                    } returns emptyList()

                    val result = queryService.getMonthlySchedules(clubId, userId)

                    result.size shouldBe 1
                    result[0].isMeeting shouldBe false
                }
            }
        }

        describe("getUnreadNotice") {
            context("읽지 않은 공지가 있는 경우") {
                it("읽지 않은 최신 공지 1건을 반환한다") {
                    val noticeBoard = BoardTestFixture.create(type = BoardType.NOTICE)
                    val notice = PostTestFixture.create(board = noticeBoard)

                    every { clubMemberReader.findByClubIdAndUserId(clubId, userId) } returns clubMember
                    every { postReader.findRecentByBoardTypeSince(BoardType.NOTICE, any()) } returns listOf(notice)
                    every { noticeReadReader.findReadPostIdsByUserId(userId, any()) } returns emptySet()

                    val result = queryService.getUnreadNotice(clubId, userId)

                    result shouldNotBe null
                }
            }

            context("모든 공지를 읽은 경우") {
                it("null을 반환한다") {
                    val noticeBoard = BoardTestFixture.create(type = BoardType.NOTICE)
                    val notice = PostTestFixture.create(board = noticeBoard)

                    every { clubMemberReader.findByClubIdAndUserId(clubId, userId) } returns clubMember
                    every { postReader.findRecentByBoardTypeSince(BoardType.NOTICE, any()) } returns listOf(notice)
                    every { noticeReadReader.findReadPostIdsByUserId(userId, any()) } returns setOf(notice.id)

                    val result = queryService.getUnreadNotice(clubId, userId)

                    result shouldBe null
                }
            }

            context("2주 내 공지가 없는 경우") {
                it("null을 반환한다") {
                    every { clubMemberReader.findByClubIdAndUserId(clubId, userId) } returns clubMember
                    every { postReader.findRecentByBoardTypeSince(BoardType.NOTICE, any()) } returns emptyList()
                    every { noticeReadReader.findReadPostIdsByUserId(userId, any()) } returns emptySet()

                    val result = queryService.getUnreadNotice(clubId, userId)

                    result shouldBe null
                }
            }
        }
    })
