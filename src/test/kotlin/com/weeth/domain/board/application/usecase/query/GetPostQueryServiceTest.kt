package com.weeth.domain.board.application.usecase.query

import com.weeth.domain.board.application.exception.BoardNotFoundException
import com.weeth.domain.board.application.exception.NoSearchResultException
import com.weeth.domain.board.application.exception.PageNotFoundException
import com.weeth.domain.board.application.exception.PostNotFoundException
import com.weeth.domain.board.application.mapper.PostMapper
import com.weeth.domain.board.domain.enums.BoardType
import com.weeth.domain.board.domain.repository.BoardRepository
import com.weeth.domain.board.domain.repository.PostRepository
import com.weeth.domain.board.fixture.BoardTestFixture
import com.weeth.domain.board.fixture.PostTestFixture
import com.weeth.domain.club.domain.enums.MemberRole
import com.weeth.domain.club.domain.repository.ClubMemberReader
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.club.fixture.ClubMemberTestFixture
import com.weeth.domain.comment.application.dto.response.CommentResponse
import com.weeth.domain.comment.application.usecase.query.GetCommentQueryService
import com.weeth.domain.comment.domain.repository.CommentReader
import com.weeth.domain.file.application.dto.response.FileResponse
import com.weeth.domain.file.application.mapper.FileMapper
import com.weeth.domain.file.domain.entity.File
import com.weeth.domain.file.domain.enums.FileOwnerType
import com.weeth.domain.file.domain.enums.FileStatus
import com.weeth.domain.file.domain.repository.FileReader
import com.weeth.domain.user.application.dto.response.UserInfo
import com.weeth.domain.user.fixture.UserTestFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.SliceImpl
import java.time.LocalDateTime

class GetPostQueryServiceTest :
    DescribeSpec({
        val postRepository = mockk<PostRepository>()
        val boardRepository = mockk<BoardRepository>()
        val clubMemberPolicy = mockk<ClubMemberPolicy>(relaxed = true)
        val clubMemberReader = mockk<ClubMemberReader>()
        val commentReader = mockk<CommentReader>()
        val getCommentQueryService = mockk<GetCommentQueryService>()
        val fileReader = mockk<FileReader>()
        val fileMapper = mockk<FileMapper>()
        val postMapper = mockk<PostMapper>()

        val queryService =
            GetPostQueryService(
                postRepository,
                boardRepository,
                clubMemberPolicy,
                clubMemberReader,
                commentReader,
                getCommentQueryService,
                fileReader,
                fileMapper,
                postMapper,
            )

        val clubId = 1L
        val userId = 1L

        beforeTest {
            clearMocks(
                postRepository,
                boardRepository,
                clubMemberPolicy,
                clubMemberReader,
                commentReader,
                getCommentQueryService,
                fileReader,
                fileMapper,
                postMapper,
            )
        }

        describe("findPost") {
            it("존재하지 않는 게시글이면 예외를 던진다") {
                val member = ClubMemberTestFixture.createActiveMember()
                every { clubMemberPolicy.getActiveMember(clubId, userId) } returns member
                every { postRepository.findByIdAndIsDeletedFalse(1L) } returns null

                shouldThrow<PostNotFoundException> {
                    queryService.findPost(clubId, userId, 1L)
                }
            }

            it("댓글/파일을 포함한 상세 응답을 반환한다") {
                val user = UserTestFixture.createActiveUser1(1L)
                val board = BoardTestFixture.create(name = "일반", type = BoardType.GENERAL)
                val actualClubId = board.club.id
                val member = ClubMemberTestFixture.createActiveMember(club = board.club, user = user)
                val post =
                    PostTestFixture.create(
                        title = "제목",
                        content = "내용",
                        user = user,
                        board = board,
                    )
                val comments = listOf(mockk<CommentResponse>())
                val fileResponses =
                    listOf(
                        FileResponse(
                            fileId = 1L,
                            fileName = "a.png",
                            fileUrl = "https://cdn/a.png",
                            storageKey = "POST/2026-02/550e8400-e29b-41d4-a716-446655440000_a.png",
                            fileSize = 100,
                            contentType = "image/png",
                            status = FileStatus.UPLOADED,
                        ),
                    )
                val files =
                    listOf(
                        File.createUploaded(
                            fileName = "a.png",
                            storageKey = "POST/2026-02/550e8400-e29b-41d4-a716-446655440000_a.png",
                            fileSize = 100,
                            contentType = "image/png",
                            ownerType = FileOwnerType.POST,
                            ownerId = 1L,
                        ),
                    )
                val detail =
                    com.weeth.domain.board.application.dto.response.PostDetailResponse(
                        id = 1L,
                        author = UserInfo(id = 1L, name = "적순", profileImageUrl = null, role = MemberRole.USER),
                        title = "제목",
                        content = "내용",
                        time = LocalDateTime.now(),
                        commentCount = 1,
                        comments = comments,
                        fileUrls = fileResponses,
                    )

                every { clubMemberPolicy.getActiveMember(actualClubId, userId) } returns member
                every { postRepository.findByIdAndIsDeletedFalse(1L) } returns post
                every { commentReader.findAllByPostId(any<Long>()) } returns emptyList()
                every { clubMemberReader.findAllByClubIdAndUserIds(actualClubId, any()) } returns listOf(member)
                every { getCommentQueryService.toCommentTreeResponses(any(), any()) } returns comments
                every { fileReader.findAll(FileOwnerType.POST, any<Long>(), any()) } returns files
                every { postMapper.toDetailResponse(post, member, comments, fileResponses) } returns detail
                every { fileMapper.toFileResponse(files.first()) } returns fileResponses.first()

                val result = queryService.findPost(actualClubId, userId, 1L)

                result.id shouldBe 1L
                result.comments.size shouldBe 1
                result.fileUrls.size shouldBe 1
            }

            it("비공개 게시판 게시글은 일반 멤버에게 노출하지 않는다") {
                val user = UserTestFixture.createActiveUser1(1L)
                val privateBoard = BoardTestFixture.create(name = "비공개", type = BoardType.GENERAL)
                val actualClubId = privateBoard.club.id
                privateBoard.updateConfig(privateBoard.config.copy(isPrivate = true))
                val member = ClubMemberTestFixture.createActiveMember(club = privateBoard.club, user = user)
                val post =
                    PostTestFixture.create(
                        title = "제목",
                        content = "내용",
                        user = user,
                        board = privateBoard,
                    )

                every { clubMemberPolicy.getActiveMember(actualClubId, userId) } returns member
                every { postRepository.findByIdAndIsDeletedFalse(1L) } returns post

                shouldThrow<PostNotFoundException> {
                    queryService.findPost(actualClubId, userId, 1L)
                }
            }

            it("삭제된 게시판의 게시글은 조회할 수 없다") {
                val user = UserTestFixture.createActiveUser1(1L)
                val deletedBoard =
                    BoardTestFixture
                        .create(
                            name = "삭제",
                            type = BoardType.GENERAL,
                        ).also { it.markDeleted() }
                val actualClubId = deletedBoard.club.id
                val member = ClubMemberTestFixture.createActiveMember(club = deletedBoard.club, user = user)
                val post =
                    PostTestFixture.create(
                        title = "제목",
                        content = "내용",
                        user = user,
                        board = deletedBoard,
                    )

                every { clubMemberPolicy.getActiveMember(actualClubId, userId) } returns member
                every { postRepository.findByIdAndIsDeletedFalse(1L) } returns post

                shouldThrow<PostNotFoundException> {
                    queryService.findPost(actualClubId, userId, 1L)
                }
            }
        }

        describe("searchPosts") {
            it("검색 결과가 없으면 예외를 던진다") {
                val pageable = PageRequest.of(0, 10)
                val board = BoardTestFixture.create(name = "일반", type = BoardType.GENERAL)
                val member = ClubMemberTestFixture.createActiveMember()
                every { clubMemberPolicy.getActiveMember(clubId, userId) } returns member
                every { boardRepository.findByIdAndClubIdAndIsDeletedFalse(1L, clubId) } returns board
                every { postRepository.searchByBoardId(1L, "키워드", any()) } returns
                    SliceImpl(emptyList(), pageable, false)

                shouldThrow<NoSearchResultException> {
                    queryService.searchPosts(clubId, userId, 1L, "키워드", 0, 10)
                }
            }

            it("비공개 게시판은 일반 멤버가 검색할 수 없다") {
                val privateBoard = BoardTestFixture.create(name = "비공개", type = BoardType.GENERAL)
                privateBoard.updateConfig(privateBoard.config.copy(isPrivate = true))
                val member = ClubMemberTestFixture.createActiveMember()
                every { clubMemberPolicy.getActiveMember(clubId, userId) } returns member
                every { boardRepository.findByIdAndClubIdAndIsDeletedFalse(1L, clubId) } returns privateBoard

                shouldThrow<BoardNotFoundException> {
                    queryService.searchPosts(clubId, userId, 1L, "키워드", 0, 10)
                }
            }
        }

        describe("validatePage") {
            it("음수 페이지면 예외를 던진다") {
                val member = ClubMemberTestFixture.createActiveMember()
                every { clubMemberPolicy.getActiveMember(clubId, userId) } returns member
                shouldThrow<PageNotFoundException> {
                    queryService.findPosts(clubId, userId, 1L, -1, 10)
                }
            }

            it("pageSize가 0이면 예외를 던진다") {
                val member = ClubMemberTestFixture.createActiveMember()
                every { clubMemberPolicy.getActiveMember(clubId, userId) } returns member
                shouldThrow<PageNotFoundException> {
                    queryService.findPosts(clubId, userId, 1L, 0, 0)
                }
            }

            it("pageSize가 최대값을 초과하면 예외를 던진다") {
                val member = ClubMemberTestFixture.createActiveMember()
                every { clubMemberPolicy.getActiveMember(clubId, userId) } returns member
                shouldThrow<PageNotFoundException> {
                    queryService.findPosts(clubId, userId, 1L, 0, 51)
                }
            }
        }

        describe("findPosts") {
            it("목록 조회 시 mapper를 통해 응답으로 변환한다") {
                val user = UserTestFixture.createActiveUser1(1L)
                val board = BoardTestFixture.create(name = "일반", type = BoardType.GENERAL)
                val member = ClubMemberTestFixture.createActiveMember(user = user)
                val post =
                    PostTestFixture.create(
                        title = "제목",
                        content = "내용",
                        user = user,
                        board = board,
                    )
                val pageable = PageRequest.of(0, 10)
                val postSlice = SliceImpl(listOf(post), pageable, false)
                val response =
                    com.weeth.domain.board.application.dto.response.PostListResponse(
                        id = 10L,
                        author = UserInfo(id = 1L, name = "적순", profileImageUrl = null, role = MemberRole.USER),
                        title = "제목",
                        content = "내용",
                        time = LocalDateTime.now(),
                        commentCount = 0,
                        hasFile = false,
                        isNew = false,
                    )

                every { clubMemberPolicy.getActiveMember(clubId, userId) } returns member
                every { boardRepository.findByIdAndClubIdAndIsDeletedFalse(1L, clubId) } returns board
                every { postRepository.findAllActiveByBoardId(1L, any()) } returns postSlice
                every { fileReader.findAll(FileOwnerType.POST, any<List<Long>>(), any()) } returns emptyList()
                every { clubMemberReader.findAllByClubIdAndUserIds(clubId, any()) } returns listOf(member)
                every { postMapper.toListResponse(any(), any(), any(), any()) } returns response

                val result = queryService.findPosts(clubId, userId, 1L, 0, 10)

                result.content.size shouldBe 1
                verify(exactly = 1) { fileReader.findAll(FileOwnerType.POST, any<List<Long>>(), any()) }
            }
        }
    })
