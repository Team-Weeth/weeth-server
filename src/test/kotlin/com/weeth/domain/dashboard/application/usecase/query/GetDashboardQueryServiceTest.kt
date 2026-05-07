package com.weeth.domain.dashboard.application.usecase.query

import com.weeth.domain.board.domain.enums.BoardType
import com.weeth.domain.board.domain.repository.BoardReader
import com.weeth.domain.board.domain.repository.PostLikeReader
import com.weeth.domain.board.domain.repository.PostReader
import com.weeth.domain.board.domain.vo.BoardConfig
import com.weeth.domain.board.fixture.BoardTestFixture
import com.weeth.domain.board.fixture.PostTestFixture
import com.weeth.domain.club.application.exception.ClubMemberNotFoundException
import com.weeth.domain.club.application.exception.MemberNotActiveException
import com.weeth.domain.club.domain.enums.MemberRole
import com.weeth.domain.club.domain.enums.MemberStatus
import com.weeth.domain.club.domain.repository.ClubMemberReader
import com.weeth.domain.club.domain.repository.ClubReader
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.club.fixture.ClubTestFixture
import com.weeth.domain.dashboard.application.mapper.DashboardMapper
import com.weeth.domain.dashboard.domain.enums.ScheduleType
import com.weeth.domain.file.application.mapper.FileMapper
import com.weeth.domain.file.domain.enums.FileOwnerType
import com.weeth.domain.file.domain.port.FileAccessUrlPort
import com.weeth.domain.file.domain.repository.FileReader
import com.weeth.domain.schedule.domain.repository.EventReader
import com.weeth.domain.schedule.fixture.ScheduleTestFixture
import com.weeth.domain.session.domain.repository.SessionReader
import com.weeth.domain.session.fixture.SessionTestFixture
import com.weeth.domain.user.domain.repository.UserReader
import com.weeth.domain.user.fixture.UserTestFixture
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
        val boardReader = mockk<BoardReader>()
        val postLikeReader = mockk<PostLikeReader>()
        val clubReader = mockk<ClubReader>()
        val clubMemberReader = mockk<ClubMemberReader>()
        val clubMemberPolicy = ClubMemberPolicy(clubMemberReader)
        val eventReader = mockk<EventReader>()
        val sessionReader = mockk<SessionReader>()
        val postReader = mockk<PostReader>()
        val fileReader = mockk<FileReader>()
        val userReader = mockk<UserReader>()
        val fileMapper = mockk<FileMapper>()
        val fileAccessUrlPort = mockk<FileAccessUrlPort>()
        val dashboardMapper = DashboardMapper(fileMapper, fileAccessUrlPort)

        val queryService =
            GetDashboardQueryService(
                boardReader = boardReader,
                postLikeReader = postLikeReader,
                clubReader = clubReader,
                clubMemberReader = clubMemberReader,
                clubMemberPolicy = clubMemberPolicy,
                eventReader = eventReader,
                sessionReader = sessionReader,
                postReader = postReader,
                fileReader = fileReader,
                userReader = userReader,
                dashboardMapper = dashboardMapper,
            )

        val clubId = 1L
        val userId = 1L
        val club = ClubTestFixture.createClub()
        val clubMember = ClubTestFixture.createClubMember(club = club)
        val user = UserTestFixture.createActiveUser1(1L)

        beforeTest {
            clearMocks(
                boardReader,
                postLikeReader,
                clubReader,
                clubMemberReader,
                eventReader,
                sessionReader,
                postReader,
                fileReader,
                userReader,
                fileMapper,
            )
        }

        describe("getHome") {
            context("활성 멤버인 경우") {
                it("홈 정보를 반환한다") {
                    every { clubReader.getClubById(clubId) } returns club
                    every { clubMemberReader.findByClubIdAndUserId(clubId, userId) } returns clubMember
                    every { clubMemberReader.countActiveByClubId(clubId) } returns 10L
                    every { eventReader.findByClubIdAndDateRange(clubId, any(), any()) } returns emptyList()
                    every {
                        sessionReader.findAllByClubIdAndStartBetween(clubId, any(), any())
                    } returns emptyList()
                    every { clubMemberReader.findActiveByUserId(userId) } returns listOf(clubMember)
                    every { userReader.getById(userId) } returns user

                    val result = queryService.getHome(clubId, userId)

                    result shouldNotBe null
                    result.club.memberCount shouldBe 10L
                    result.myClubs.size shouldBe 1
                }
            }

            context("멤버가 아닌 경우") {
                it("ClubMemberNotFoundException을 던진다") {
                    every { clubReader.getClubById(clubId) } returns club
                    every { clubMemberReader.findByClubIdAndUserId(clubId, userId) } returns null

                    shouldThrow<ClubMemberNotFoundException> {
                        queryService.getHome(clubId, userId)
                    }
                }
            }

            context("비활성 멤버인 경우") {
                it("MemberNotActiveException을 던진다") {
                    val inactiveMember =
                        ClubTestFixture.createClubMember(
                            club = club,
                            memberStatus = MemberStatus.BANNED,
                        )
                    every { clubReader.getClubById(clubId) } returns club
                    every { clubMemberReader.findByClubIdAndUserId(clubId, userId) } returns inactiveMember

                    shouldThrow<MemberNotActiveException> {
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
                    every { eventReader.findByClubIdAndDateRange(clubId, any(), any()) } returns listOf(event)
                    every {
                        sessionReader.findAllByClubIdAndStartBetween(clubId, any(), any())
                    } returns listOf(session)
                    every { clubMemberReader.findActiveByUserId(userId) } returns listOf(clubMember)
                    every { userReader.getById(userId) } returns user

                    val result = queryService.getHome(clubId, userId)

                    result.todaySchedules.size shouldBe 2
                    result.todaySchedules[0].type shouldBe ScheduleType.EVENT
                    result.todaySchedules[1].type shouldBe ScheduleType.SESSION
                }
            }
        }

        describe("getRecentPosts") {
            val memberWithUser = ClubTestFixture.createClubMember(club = club, user = user)

            context("멤버인 경우") {
                it("공지 제외한 접근 가능한 게시판의 최신 게시글을 반환한다") {
                    val board = BoardTestFixture.create(id = 10L, type = BoardType.GENERAL)
                    val post = PostTestFixture.create(board = board, clubMember = memberWithUser)
                    val pageable = PageRequest.of(0, 10)
                    val slice = SliceImpl(listOf(post), pageable, false)

                    every { clubMemberReader.findByClubIdAndUserId(clubId, userId) } returns memberWithUser
                    every { boardReader.findAllActiveByClubId(clubId) } returns listOf(board)
                    every { postReader.findRecentByBoardIds(listOf(board.id), any()) } returns slice
                    every { fileReader.findAll(FileOwnerType.POST, any<List<Long>>()) } returns emptyList()
                    every { postLikeReader.findLikedPostIds(listOf(post.id), userId) } returns emptySet()

                    val result = queryService.getRecentPosts(clubId, userId, 0, 10)

                    result.content.size shouldBe 1
                    result.content[0].boardId shouldBe board.id
                    result.content[0].fileUrls.isEmpty() shouldBe true
                    result.content[0].like.isLiked shouldBe false
                }
            }

            context("비공개 게시판이 있는 경우") {
                val privateBoard =
                    BoardTestFixture.create(
                        id = 11L,
                        type = BoardType.GENERAL,
                        config = BoardConfig(isPrivate = true),
                    )

                it("일반 멤버에게는 비공개 게시판 글이 포함되지 않는다") {
                    val publicBoard = BoardTestFixture.create(id = 10L, type = BoardType.GENERAL)
                    val post = PostTestFixture.create(board = publicBoard, clubMember = memberWithUser)
                    val pageable = PageRequest.of(0, 10)
                    val slice = SliceImpl(listOf(post), pageable, false)

                    every { clubMemberReader.findByClubIdAndUserId(clubId, userId) } returns memberWithUser
                    every { boardReader.findAllActiveByClubId(clubId) } returns listOf(publicBoard, privateBoard)
                    every { postReader.findRecentByBoardIds(listOf(publicBoard.id), any()) } returns slice
                    every { fileReader.findAll(FileOwnerType.POST, any<List<Long>>()) } returns emptyList()
                    every { postLikeReader.findLikedPostIds(listOf(post.id), userId) } returns emptySet()

                    val result = queryService.getRecentPosts(clubId, userId, 0, 10)

                    result.content.size shouldBe 1
                }

                it("ADMIN 멤버에게는 비공개 게시판 글이 포함된다") {
                    val adminMember =
                        ClubTestFixture.createClubMember(
                            club = club,
                            user = user,
                            memberRole = MemberRole.ADMIN,
                        )
                    val post = PostTestFixture.create(board = privateBoard, clubMember = adminMember)
                    val pageable = PageRequest.of(0, 10)
                    val slice = SliceImpl(listOf(post), pageable, false)

                    every { clubMemberReader.findByClubIdAndUserId(clubId, userId) } returns adminMember
                    every { boardReader.findAllActiveByClubId(clubId) } returns listOf(privateBoard)
                    every { postReader.findRecentByBoardIds(listOf(privateBoard.id), any()) } returns slice
                    every { fileReader.findAll(FileOwnerType.POST, any<List<Long>>()) } returns emptyList()
                    every { postLikeReader.findLikedPostIds(listOf(post.id), userId) } returns emptySet()

                    val result = queryService.getRecentPosts(clubId, userId, 0, 10)

                    result.content.size shouldBe 1
                }
            }

            context("접근 가능한 게시판이 없는 경우") {
                it("빈 Slice를 반환한다") {
                    every { clubMemberReader.findByClubIdAndUserId(clubId, userId) } returns memberWithUser
                    every { boardReader.findAllActiveByClubId(clubId) } returns emptyList()

                    val result = queryService.getRecentPosts(clubId, userId, 0, 10)

                    result.content.isEmpty() shouldBe true
                    result.hasNext() shouldBe false
                }
            }

            context("멤버가 아닌 경우") {
                it("ClubMemberNotFoundException을 던진다") {
                    every { clubMemberReader.findByClubIdAndUserId(clubId, userId) } returns null

                    shouldThrow<ClubMemberNotFoundException> {
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
                    every { postReader.findRecentByClubIdAndBoardType(clubId, BoardType.NOTICE, any()) } returns slice

                    val result = queryService.getRecentNotices(clubId, userId, 5)

                    result.size shouldBe 1
                }
            }
        }

        describe("getMonthlySchedules") {
            context("멤버인 경우") {
                it("월간 일정 목록을 시작 시간순으로 반환한다") {
                    val event = ScheduleTestFixture.createEvent(id = 1L)

                    every { clubMemberReader.findByClubIdAndUserId(clubId, userId) } returns clubMember
                    every { eventReader.findByClubIdAndDateRange(clubId, any(), any()) } returns listOf(event)
                    every {
                        sessionReader.findAllByClubIdAndStartBetween(clubId, any(), any())
                    } returns emptyList()

                    val result = queryService.getMonthlySchedules(clubId, userId)

                    result.size shouldBe 1
                    result[0].type shouldBe ScheduleType.EVENT
                }
            }
        }

        describe("getUnreadNotice") {
            context("읽지 않은 공지가 있는 경우") {
                it("읽지 않은 최신 공지 1건을 반환한다") {
                    val noticeBoard = BoardTestFixture.create(type = BoardType.NOTICE)
                    val notice = PostTestFixture.create(board = noticeBoard)

                    every { clubMemberReader.findByClubIdAndUserId(clubId, userId) } returns clubMember
                    every { postReader.findFirstUnreadNoticeSince(clubId, clubMember.id, BoardType.NOTICE, any()) } returns
                        notice

                    val result = queryService.getUnreadNotice(clubId, userId)

                    result shouldNotBe null
                }
            }

            context("모든 공지를 읽은 경우") {
                it("null을 반환한다") {
                    every { clubMemberReader.findByClubIdAndUserId(clubId, userId) } returns clubMember
                    every { postReader.findFirstUnreadNoticeSince(clubId, clubMember.id, BoardType.NOTICE, any()) } returns
                        null

                    val result = queryService.getUnreadNotice(clubId, userId)

                    result shouldBe null
                }
            }

            context("2주 내 공지가 없는 경우") {
                it("null을 반환한다") {
                    every { clubMemberReader.findByClubIdAndUserId(clubId, userId) } returns clubMember
                    every { postReader.findFirstUnreadNoticeSince(clubId, clubMember.id, BoardType.NOTICE, any()) } returns
                        null

                    val result = queryService.getUnreadNotice(clubId, userId)

                    result shouldBe null
                }
            }
        }
    })
