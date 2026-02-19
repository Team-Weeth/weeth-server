package com.weeth.domain.board.application.usecase.query

import com.weeth.domain.board.application.exception.BoardNotFoundException
import com.weeth.domain.board.application.exception.NoSearchResultException
import com.weeth.domain.board.application.exception.PageNotFoundException
import com.weeth.domain.board.application.exception.PostNotFoundException
import com.weeth.domain.board.application.mapper.PostMapper
import com.weeth.domain.board.domain.entity.Board
import com.weeth.domain.board.domain.entity.Post
import com.weeth.domain.board.domain.entity.enums.BoardType
import com.weeth.domain.board.domain.repository.BoardRepository
import com.weeth.domain.board.domain.repository.PostRepository
import com.weeth.domain.comment.application.dto.response.CommentResponse
import com.weeth.domain.comment.application.usecase.query.GetCommentQueryService
import com.weeth.domain.comment.domain.repository.CommentReader
import com.weeth.domain.file.application.dto.response.FileResponse
import com.weeth.domain.file.application.mapper.FileMapper
import com.weeth.domain.file.domain.entity.File
import com.weeth.domain.file.domain.entity.FileOwnerType
import com.weeth.domain.file.domain.entity.FileStatus
import com.weeth.domain.file.domain.repository.FileReader
import com.weeth.domain.user.domain.entity.enums.Role
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
        val commentReader = mockk<CommentReader>()
        val getCommentQueryService = mockk<GetCommentQueryService>()
        val fileReader = mockk<FileReader>()
        val fileMapper = mockk<FileMapper>()
        val postMapper = mockk<PostMapper>()

        val queryService =
            GetPostQueryService(
                postRepository,
                boardRepository,
                commentReader,
                getCommentQueryService,
                fileReader,
                fileMapper,
                postMapper,
            )

        beforeTest {
            clearMocks(
                postRepository,
                boardRepository,
                commentReader,
                getCommentQueryService,
                fileReader,
                fileMapper,
                postMapper,
            )
        }

        describe("findPost") {
            it("존재하지 않는 게시글이면 예외를 던진다") {
                every { postRepository.findByIdAndIsDeletedFalse(1L) } returns null

                shouldThrow<PostNotFoundException> {
                    queryService.findPost(1L, Role.USER)
                }
            }

            it("댓글/파일을 포함한 상세 응답을 반환한다") {
                val user = UserTestFixture.createActiveUser1(1L)
                val board = Board(id = 1L, name = "일반", type = BoardType.GENERAL)
                val post = Post(id = 1L, title = "제목", content = "내용", user = user, board = board, commentCount = 1)
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
                        name = "적순",
                        role = Role.USER,
                        title = "제목",
                        content = "내용",
                        time = LocalDateTime.now(),
                        commentCount = 1,
                        comments = comments,
                        fileUrls = fileResponses,
                    )

                every { postRepository.findByIdAndIsDeletedFalse(1L) } returns post
                every { commentReader.findAllByPostId(1L) } returns emptyList()
                every { getCommentQueryService.toCommentTreeResponses(any()) } returns comments
                every { fileReader.findAll(FileOwnerType.POST, 1L, any()) } returns files
                every { postMapper.toDetailResponse(post, comments, fileResponses) } returns detail
                every { fileMapper.toFileResponse(files.first()) } returns fileResponses.first()

                val result = queryService.findPost(1L, Role.USER)

                result.id shouldBe 1L
                result.comments.size shouldBe 1
                result.fileUrls.size shouldBe 1
            }

            it("비공개 게시판 게시글은 일반/익명에게 노출하지 않는다") {
                val user = UserTestFixture.createActiveUser1(1L)
                val privateBoard = Board(id = 2L, name = "비공개", type = BoardType.GENERAL)
                privateBoard.updateConfig(privateBoard.config.copy(isPrivate = true))
                val post = Post(id = 1L, title = "제목", content = "내용", user = user, board = privateBoard, commentCount = 0)

                every { postRepository.findByIdAndIsDeletedFalse(1L) } returns post

                shouldThrow<PostNotFoundException> {
                    queryService.findPost(1L, Role.USER)
                }
            }

            it("삭제된 게시판의 게시글은 조회할 수 없다") {
                val user = UserTestFixture.createActiveUser1(1L)
                val deletedBoard = Board(id = 3L, name = "삭제", type = BoardType.GENERAL, isDeleted = true)
                val post = Post(id = 1L, title = "제목", content = "내용", user = user, board = deletedBoard, commentCount = 0)

                every { postRepository.findByIdAndIsDeletedFalse(1L) } returns post

                shouldThrow<PostNotFoundException> {
                    queryService.findPost(1L, Role.USER)
                }
            }
        }

        describe("searchPosts") {
            it("검색 결과가 없으면 예외를 던진다") {
                val pageable = PageRequest.of(0, 10)
                val board = Board(id = 1L, name = "일반", type = BoardType.GENERAL)
                every { boardRepository.findByIdAndIsDeletedFalse(1L) } returns board
                every { postRepository.searchByBoardId(1L, "키워드", any()) } returns SliceImpl(emptyList(), pageable, false)

                shouldThrow<NoSearchResultException> {
                    queryService.searchPosts(1L, "키워드", 0, 10, Role.USER)
                }
            }

            it("비공개 게시판은 일반/익명이 검색할 수 없다") {
                val privateBoard = Board(id = 1L, name = "비공개", type = BoardType.GENERAL)
                privateBoard.updateConfig(privateBoard.config.copy(isPrivate = true))
                every { boardRepository.findByIdAndIsDeletedFalse(1L) } returns privateBoard

                shouldThrow<BoardNotFoundException> {
                    queryService.searchPosts(1L, "키워드", 0, 10, Role.USER)
                }
            }
        }

        describe("validatePage") {
            it("음수 페이지면 예외를 던진다") {
                shouldThrow<PageNotFoundException> {
                    queryService.findPosts(1L, -1, 10, Role.USER)
                }
            }

            it("pageSize가 0이면 예외를 던진다") {
                shouldThrow<PageNotFoundException> {
                    queryService.findPosts(1L, 0, 0, Role.USER)
                }
            }

            it("pageSize가 최대값을 초과하면 예외를 던진다") {
                shouldThrow<PageNotFoundException> {
                    queryService.findPosts(1L, 0, 51, Role.USER)
                }
            }
        }

        describe("findPosts") {
            it("목록 조회 시 mapper를 통해 응답으로 변환한다") {
                val user = UserTestFixture.createActiveUser1(1L)
                val board = Board(id = 1L, name = "일반", type = BoardType.GENERAL)
                val post = Post(id = 10L, title = "제목", content = "내용", user = user, board = board)
                val pageable = PageRequest.of(0, 10)
                val postSlice = SliceImpl(listOf(post), pageable, false)
                val response =
                    com.weeth.domain.board.application.dto.response.PostListResponse(
                        id = 10L,
                        name = "적순",
                        role = Role.USER,
                        title = "제목",
                        content = "내용",
                        time = LocalDateTime.now(),
                        commentCount = 0,
                        hasFile = false,
                        isNew = false,
                    )

                every { boardRepository.findByIdAndIsDeletedFalse(1L) } returns board
                every { postRepository.findAllActiveByBoardId(1L, any()) } returns postSlice
                every { fileReader.findAll(FileOwnerType.POST, any<List<Long>>(), any()) } returns emptyList()
                every { postMapper.toListResponse(any(), any(), any()) } returns response

                val result = queryService.findPosts(1L, 0, 10, Role.USER)

                result.content.size shouldBe 1
                result.content.first().id shouldBe 10L
                verify(exactly = 1) { fileReader.findAll(FileOwnerType.POST, listOf(10L), any()) }
            }
        }
    })
