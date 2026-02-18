package com.weeth.domain.board.application.usecase.query

import com.weeth.domain.board.application.exception.NoSearchResultException
import com.weeth.domain.board.application.exception.PageNotFoundException
import com.weeth.domain.board.application.exception.PostNotFoundException
import com.weeth.domain.board.application.mapper.PostMapper
import com.weeth.domain.board.domain.entity.Board
import com.weeth.domain.board.domain.entity.Post
import com.weeth.domain.board.domain.entity.enums.BoardType
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
import java.util.Optional

class GetPostQueryServiceTest :
    DescribeSpec({
        val postRepository = mockk<PostRepository>()
        val commentReader = mockk<CommentReader>()
        val getCommentQueryService = mockk<GetCommentQueryService>()
        val fileReader = mockk<FileReader>()
        val fileMapper = mockk<FileMapper>()
        val postMapper = mockk<PostMapper>()

        val queryService =
            GetPostQueryService(
                postRepository,
                commentReader,
                getCommentQueryService,
                fileReader,
                fileMapper,
                postMapper,
            )

        beforeTest {
            clearMocks(
                postRepository,
                commentReader,
                getCommentQueryService,
                fileReader,
                fileMapper,
                postMapper,
            )
        }

        describe("findPost") {
            it("존재하지 않는 게시글이면 예외를 던진다") {
                every { postRepository.findById(1L) } returns Optional.empty()

                shouldThrow<PostNotFoundException> {
                    queryService.findPost(1L)
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

                every { postRepository.findById(1L) } returns Optional.of(post)
                every { commentReader.findAllByPostId(1L) } returns emptyList()
                every { getCommentQueryService.toCommentTreeResponses(any()) } returns comments
                every { fileReader.findAll(FileOwnerType.POST, 1L, any()) } returns files
                every { postMapper.toDetailResponse(post, comments, fileResponses) } returns detail
                every { fileMapper.toFileResponse(files.first()) } returns fileResponses.first()

                val result = queryService.findPost(1L)

                result.id shouldBe 1L
                result.comments.size shouldBe 1
                result.fileUrls.size shouldBe 1
            }
        }

        describe("searchPosts") {
            it("검색 결과가 없으면 예외를 던진다") {
                val pageable = PageRequest.of(0, 10)
                every { postRepository.searchByBoardId(1L, "키워드", any()) } returns SliceImpl(emptyList(), pageable, false)

                shouldThrow<NoSearchResultException> {
                    queryService.searchPosts(1L, "키워드", 0, 10)
                }
            }
        }

        describe("validatePage") {
            it("음수 페이지면 예외를 던진다") {
                shouldThrow<PageNotFoundException> {
                    queryService.findPosts(1L, -1, 10)
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

                every { postRepository.findAllByBoardId(1L, any()) } returns postSlice
                every { fileReader.findAll(FileOwnerType.POST, any<List<Long>>(), any()) } returns emptyList()
                every { fileReader.findAll(FileOwnerType.POST, 10L, any()) } returns emptyList()
                every { postMapper.toListResponse(any(), any(), any()) } returns response

                val result = queryService.findPosts(1L, 0, 10)

                result.content.size shouldBe 1
                result.content.first().id shouldBe 10L
                verify(exactly = 1) { fileReader.findAll(FileOwnerType.POST, listOf(10L), any()) }
            }
        }
    })
