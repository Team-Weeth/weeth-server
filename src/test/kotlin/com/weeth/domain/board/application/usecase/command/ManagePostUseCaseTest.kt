package com.weeth.domain.board.application.usecase.command

import com.weeth.domain.board.application.dto.request.CreatePostRequest
import com.weeth.domain.board.application.dto.request.UpdatePostRequest
import com.weeth.domain.board.application.dto.response.PostSaveResponse
import com.weeth.domain.board.application.exception.BoardNotFoundException
import com.weeth.domain.board.application.exception.CategoryAccessDeniedException
import com.weeth.domain.board.application.mapper.PostMapper
import com.weeth.domain.board.domain.entity.Board
import com.weeth.domain.board.domain.entity.Post
import com.weeth.domain.board.domain.entity.enums.BoardType
import com.weeth.domain.board.domain.repository.BoardRepository
import com.weeth.domain.board.domain.repository.PostRepository
import com.weeth.domain.board.domain.vo.BoardConfig
import com.weeth.domain.file.application.dto.request.FileSaveRequest
import com.weeth.domain.file.application.mapper.FileMapper
import com.weeth.domain.file.domain.entity.File
import com.weeth.domain.file.domain.entity.FileOwnerType
import com.weeth.domain.file.domain.repository.FileReader
import com.weeth.domain.file.domain.repository.FileRepository
import com.weeth.domain.user.domain.entity.enums.Role
import com.weeth.domain.user.domain.service.UserGetService
import com.weeth.domain.user.fixture.UserTestFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import java.util.Optional

class ManagePostUseCaseTest :
    DescribeSpec({
        val postRepository = mockk<PostRepository>()
        val boardRepository = mockk<BoardRepository>()
        val userGetService = mockk<UserGetService>()
        val fileRepository = mockk<FileRepository>()
        val fileReader = mockk<FileReader>()
        val fileMapper = mockk<FileMapper>()
        val postMapper = mockk<PostMapper>()

        val useCase =
            ManagePostUseCase(
                postRepository,
                boardRepository,
                userGetService,
                fileRepository,
                fileReader,
                fileMapper,
                postMapper,
            )

        beforeTest {
            clearMocks(postRepository, boardRepository, userGetService, fileRepository, fileReader, fileMapper, postMapper)
            every { postRepository.save(any()) } answers { firstArg() }
            every { fileMapper.toFileList(any(), any(), any()) } returns emptyList()
            every { fileRepository.saveAll(any<List<File>>()) } returns emptyList()
            every { fileReader.findAll(any(), any<Long>(), any()) } returns emptyList()
            every { postMapper.toSaveResponse(any()) } returns PostSaveResponse(1L)
            every { fileRepository.delete(any()) } just runs
        }

        describe("save") {
            it("일반 게시판에서 게시글을 저장한다") {
                val user = UserTestFixture.createActiveUser1(1L)
                val board = Board(id = 10L, name = "일반", type = BoardType.GENERAL)
                val request = CreatePostRequest(title = "제목", content = "내용")

                every { userGetService.find(1L) } returns user
                every { boardRepository.findById(10L) } returns Optional.of(board)

                val result = useCase.save(10L, request, 1L)

                result.id shouldBe 1L
                verify(exactly = 1) { postRepository.save(any<Post>()) }
            }

            it("ADMIN 전용 게시판에 일반 사용자가 작성하면 예외를 던진다") {
                val user = UserTestFixture.createActiveUser1(1L)
                val board =
                    Board(
                        id = 20L,
                        name = "공지",
                        type = BoardType.NOTICE,
                        config = BoardConfig(writePermission = BoardConfig.WritePermission.ADMIN),
                    )
                val request = CreatePostRequest(title = "제목", content = "내용")

                every { userGetService.find(1L) } returns user
                every { boardRepository.findById(20L) } returns Optional.of(board)

                shouldThrow<CategoryAccessDeniedException> {
                    useCase.save(20L, request, 1L)
                }

                verify(exactly = 0) { postRepository.save(any<Post>()) }
            }

            it("cardinalNumber가 전달되면 게시글에 반영된다") {
                val user = UserTestFixture.createActiveUser1(1L)
                val board = Board(id = 11L, name = "일반", type = BoardType.GENERAL)
                val request =
                    CreatePostRequest(
                        title = "게시글",
                        content = "내용",
                        cardinalNumber = 6,
                    )

                every { userGetService.find(1L) } returns user
                every { boardRepository.findById(11L) } returns Optional.of(board)

                useCase.save(11L, request, 1L)

                verify {
                    postRepository.save(
                        match {
                            it.cardinalNumber == 6
                        },
                    )
                }
            }

            it("존재하지 않는 boardId면 예외를 던진다") {
                val user = UserTestFixture.createActiveUser1(1L)
                val request = CreatePostRequest(title = "제목", content = "내용")

                every { userGetService.find(1L) } returns user
                every { boardRepository.findById(999L) } returns Optional.empty()

                shouldThrow<BoardNotFoundException> {
                    useCase.save(999L, request, 1L)
                }
            }
        }

        describe("update") {
            it("files가 null이면 기존 파일을 유지한다") {
                val user = UserTestFixture.createActiveUser1(1L)
                val board = Board(id = 1L, name = "일반", type = BoardType.GENERAL)
                val post = Post.create("제목", "내용", user, board)
                val request = UpdatePostRequest(title = "수정", content = "수정")

                every { postRepository.findById(1L) } returns Optional.of(post)

                useCase.update(1L, request, 1L)

                verify(exactly = 0) { fileReader.findAll(any(), any<Long>(), any()) }
                verify(exactly = 0) { fileRepository.saveAll(any<List<File>>()) }
            }

            it("files가 있으면 기존 파일을 soft delete 후 교체한다") {
                val user = UserTestFixture.createActiveUser1(1L)
                val board = Board(id = 1L, name = "일반", type = BoardType.GENERAL)
                val post = Post(id = 1L, title = "제목", content = "내용", user = user, board = board)
                val oldFile =
                    File.createUploaded(
                        fileName = "old.png",
                        storageKey = "POST/2026-02/550e8400-e29b-41d4-a716-446655440000_old.png",
                        fileSize = 10,
                        contentType = "image/png",
                        ownerType = FileOwnerType.POST,
                        ownerId = 1L,
                    )
                val newFiles =
                    listOf(
                        File.createUploaded(
                            fileName = "new.png",
                            storageKey = "POST/2026-02/550e8400-e29b-41d4-a716-446655440001_new.png",
                            fileSize = 10,
                            contentType = "image/png",
                            ownerType = FileOwnerType.POST,
                            ownerId = 1L,
                        ),
                    )
                val request =
                    UpdatePostRequest(
                        title = "수정",
                        content = "수정",
                        files =
                            listOf(
                                FileSaveRequest(
                                    "new.png",
                                    "POST/2026-02/550e8400-e29b-41d4-a716-446655440001_new.png",
                                    10,
                                    "image/png",
                                ),
                            ),
                    )

                every { postRepository.findById(1L) } returns Optional.of(post)
                every { fileReader.findAll(FileOwnerType.POST, 1L, any()) } returns listOf(oldFile)
                every { fileMapper.toFileList(request.files, FileOwnerType.POST, 1L) } returns newFiles
                every { fileRepository.saveAll(newFiles) } returns newFiles

                useCase.update(1L, request, 1L)

                oldFile.status.name shouldBe "DELETED"
                verify(exactly = 1) { fileRepository.saveAll(newFiles) }
            }
        }

        describe("delete") {
            it("삭제 시 첨부 파일을 soft delete하고 게시글을 삭제한다") {
                val user = UserTestFixture.createActiveUser1(1L)
                val board = Board(id = 1L, name = "일반", type = BoardType.GENERAL)
                val post = Post(id = 1L, title = "제목", content = "내용", user = user, board = board)
                val oldFile =
                    File.createUploaded(
                        fileName = "old.png",
                        storageKey = "POST/2026-02/550e8400-e29b-41d4-a716-446655440000_old.png",
                        fileSize = 10,
                        contentType = "image/png",
                        ownerType = FileOwnerType.POST,
                        ownerId = 1L,
                    )

                every { postRepository.findById(1L) } returns Optional.of(post)
                every { fileReader.findAll(FileOwnerType.POST, 1L, any()) } returns listOf(oldFile)
                every { postRepository.delete(post) } just runs

                useCase.delete(1L, 1L)

                oldFile.status.name shouldBe "DELETED"
                verify(exactly = 1) { postRepository.delete(post) }
            }
        }

        describe("owner validation") {
            it("작성자가 아니면 수정 시 예외를 던진다") {
                val owner = UserTestFixture.createActiveUser1(1L)
                val board = Board(id = 1L, name = "일반", type = BoardType.GENERAL)
                val post = Post(id = 1L, title = "제목", content = "내용", user = owner, board = board)
                val request = UpdatePostRequest(title = "수정", content = "수정")

                every { postRepository.findById(1L) } returns Optional.of(post)

                shouldThrow<com.weeth.domain.board.application.exception.PostNotOwnedException> {
                    useCase.update(1L, request, 2L)
                }
            }
        }
    })
