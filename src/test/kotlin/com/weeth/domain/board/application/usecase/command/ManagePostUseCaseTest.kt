package com.weeth.domain.board.application.usecase.command

import com.weeth.domain.board.application.dto.request.CreatePostRequest
import com.weeth.domain.board.application.dto.request.UpdatePostRequest
import com.weeth.domain.board.application.dto.response.PostSaveResponse
import com.weeth.domain.board.application.exception.BoardNotFoundException
import com.weeth.domain.board.application.exception.CategoryAccessDeniedException
import com.weeth.domain.board.application.exception.PostNotFoundException
import com.weeth.domain.board.application.exception.PostNotOwnedException
import com.weeth.domain.board.application.mapper.PostMapper
import com.weeth.domain.board.domain.entity.Post
import com.weeth.domain.board.domain.enums.BoardType
import com.weeth.domain.board.domain.repository.BoardRepository
import com.weeth.domain.board.domain.repository.PostRepository
import com.weeth.domain.board.domain.vo.BoardConfig
import com.weeth.domain.board.fixture.BoardTestFixture
import com.weeth.domain.board.fixture.PostTestFixture
import com.weeth.domain.club.domain.enums.MemberRole
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.file.application.dto.request.FileSaveRequest
import com.weeth.domain.file.application.mapper.FileMapper
import com.weeth.domain.file.domain.entity.File
import com.weeth.domain.file.domain.enums.FileOwnerType
import com.weeth.domain.file.domain.repository.FileReader
import com.weeth.domain.file.domain.repository.FileRepository
import com.weeth.domain.user.domain.repository.UserReader
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
import org.springframework.test.util.ReflectionTestUtils

class ManagePostUseCaseTest :
    DescribeSpec({
        val postRepository = mockk<PostRepository>()
        val boardRepository = mockk<BoardRepository>()
        val userReader = mockk<UserReader>()
        val clubMemberPolicy = mockk<ClubMemberPolicy>(relaxed = true)
        val fileRepository = mockk<FileRepository>()
        val fileReader = mockk<FileReader>()
        val fileMapper = mockk<FileMapper>()
        val postMapper = mockk<PostMapper>()

        val useCase =
            ManagePostUseCase(
                postRepository,
                boardRepository,
                userReader,
                clubMemberPolicy,
                fileRepository,
                fileReader,
                fileMapper,
                postMapper,
            )

        fun createUploadedPostFile(
            fileName: String,
            ownerId: Long = 1L,
        ): File =
            File.createUploaded(
                fileName = fileName,
                storageKey = "POST/2026-02/550e8400-e29b-41d4-a716-446655440000_$fileName",
                fileSize = 10,
                contentType = "image/png",
                ownerType = FileOwnerType.POST,
                ownerId = ownerId,
            )

        fun createUser(
            id: Long = 1L,
            role: Role = Role.USER,
        ): User =
            User(
                name = "적순",
                email = Email.from("test1@test.com"),
                status = Status.ACTIVE,
                role = role,
            ).apply {
                ReflectionTestUtils.setField(this, "id", id)
            }

        beforeTest {
            clearMocks(
                postRepository,
                boardRepository,
                userReader,
                clubMemberPolicy,
                fileRepository,
                fileReader,
                fileMapper,
                postMapper,
            )
            every { postRepository.save(any()) } answers { firstArg() }
            every { fileMapper.toFileList(any(), any(), any()) } returns emptyList()
            every { fileRepository.saveAll(any<List<File>>()) } returns emptyList()
            every { fileReader.findAll(any(), any<Long>(), any()) } returns emptyList()
            every { postMapper.toSaveResponse(any()) } returns PostSaveResponse(1L)
            every { fileRepository.delete(any()) } just runs
            // update/delete 공통 기본값: 작성자
            every { userReader.getById(any()) } returns UserTestFixture.createActiveUser1(1L)
        }

        describe("save") {
            it("일반 게시판에서 게시글을 저장한다") {
                val user = UserTestFixture.createActiveUser1(1L)
                val board = BoardTestFixture.create(name = "일반", type = BoardType.GENERAL)
                val request = CreatePostRequest(title = "제목", content = "내용")

                every { userReader.getById(1L) } returns user
                every { boardRepository.findByIdAndClubIdAndIsDeletedFalse(10L, 1L) } returns board

                val result = useCase.save(1L, 10L, request, 1L)

                result.id shouldBe 1L
                verify(exactly = 1) { postRepository.save(any<Post>()) }
            }

            it("ADMIN 전용 게시판에 일반 사용자가 작성하면 예외를 던진다") {
                val user = UserTestFixture.createActiveUser1(1L)
                val board =
                    BoardTestFixture.create(
                        name = "공지",
                        type = BoardType.NOTICE,
                        config = BoardConfig(writePermission = MemberRole.ADMIN),
                    )
                val request = CreatePostRequest(title = "제목", content = "내용")

                every { userReader.getById(1L) } returns user
                every { boardRepository.findByIdAndClubIdAndIsDeletedFalse(20L, 1L) } returns board

                shouldThrow<CategoryAccessDeniedException> {
                    useCase.save(1L, 20L, request, 1L)
                }

                verify(exactly = 0) { postRepository.save(any<Post>()) }
            }

            it("비공개 게시판에 일반 사용자가 작성하면 예외를 던진다") {
                val user = UserTestFixture.createActiveUser1(1L)
                val board =
                    BoardTestFixture.create(
                        name = "비공개",
                        type = BoardType.GENERAL,
                        config = BoardConfig(isPrivate = true),
                    )
                val request = CreatePostRequest(title = "제목", content = "내용")

                every { userReader.getById(1L) } returns user
                every { boardRepository.findByIdAndClubIdAndIsDeletedFalse(21L, 1L) } returns board

                shouldThrow<CategoryAccessDeniedException> {
                    useCase.save(1L, 21L, request, 1L)
                }

                verify(exactly = 0) { postRepository.save(any<Post>()) }
            }

            it("cardinalNumber가 전달되면 게시글에 반영된다") {
                val user = UserTestFixture.createActiveUser1(1L)
                val board = BoardTestFixture.create(name = "일반", type = BoardType.GENERAL)
                val request =
                    CreatePostRequest(
                        title = "게시글",
                        content = "내용",
                        cardinalNumber = 6,
                    )

                every { userReader.getById(1L) } returns user
                every { boardRepository.findByIdAndClubIdAndIsDeletedFalse(11L, 1L) } returns board

                useCase.save(1L, 11L, request, 1L)

                verify {
                    postRepository.save(
                        match { it.cardinalNumber == 6 },
                    )
                }
            }

            it("존재하지 않는 boardId면 예외를 던진다") {
                val user = UserTestFixture.createActiveUser1(1L)
                val request = CreatePostRequest(title = "제목", content = "내용")

                every { userReader.getById(1L) } returns user
                every { boardRepository.findByIdAndClubIdAndIsDeletedFalse(999L, 1L) } returns null

                shouldThrow<BoardNotFoundException> {
                    useCase.save(1L, 999L, request, 1L)
                }
            }
        }

        describe("update") {
            it("files가 null이면 기존 파일을 유지한다") {
                val user = UserTestFixture.createActiveUser1(1L)
                val board = BoardTestFixture.create(name = "일반", type = BoardType.GENERAL)
                val clubId = board.club.id
                val post = Post.create("제목", "내용", user, board)
                val request = UpdatePostRequest(title = "수정", content = "수정")

                every { userReader.getById(1L) } returns user
                every { postRepository.findActivePostById(1L) } returns post

                useCase.update(clubId, 1L, request, 1L)

                verify(exactly = 0) { fileReader.findAll(any(), any<Long>(), any()) }
                verify(exactly = 0) { fileRepository.saveAll(any<List<File>>()) }
            }

            it("files가 있으면 기존 파일을 soft delete 후 교체한다") {
                val user = UserTestFixture.createActiveUser1(1L)
                val board = BoardTestFixture.create(name = "일반", type = BoardType.GENERAL)
                val clubId = board.club.id
                val post = PostTestFixture.create(title = "제목", content = "내용", user = user, board = board)
                val oldFile = createUploadedPostFile("old.png")
                val newFiles = listOf(createUploadedPostFile("new.png"))
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

                every { userReader.getById(1L) } returns user
                every { postRepository.findActivePostById(1L) } returns post
                every { fileReader.findAll(FileOwnerType.POST, any<Long>(), any()) } returns listOf(oldFile)
                every { fileMapper.toFileList(request.files, FileOwnerType.POST, any<Long>()) } returns newFiles
                every { fileRepository.saveAll(newFiles) } returns newFiles

                useCase.update(clubId, 1L, request, 1L)

                oldFile.status.name shouldBe "DELETED"
                post.title shouldBe "수정"
                post.content shouldBe "수정"
                verify(exactly = 1) { fileRepository.saveAll(newFiles) }
            }

            it("title이 null이면 기존 제목을 유지한다") {
                val user = UserTestFixture.createActiveUser1(1L)
                val board = BoardTestFixture.create(name = "일반", type = BoardType.GENERAL)
                val clubId = board.club.id
                val post = Post.create("원래 제목", "원래 내용", user, board)
                val request = UpdatePostRequest(content = "수정된 내용")

                every { userReader.getById(1L) } returns user
                every { postRepository.findActivePostById(1L) } returns post

                useCase.update(clubId, 1L, request, 1L)

                post.title shouldBe "원래 제목"
                post.content shouldBe "수정된 내용"
            }

            it("content가 null이면 기존 내용을 유지한다") {
                val user = UserTestFixture.createActiveUser1(1L)
                val board = BoardTestFixture.create(name = "일반", type = BoardType.GENERAL)
                val clubId = board.club.id
                val post = Post.create("원래 제목", "원래 내용", user, board)
                val request = UpdatePostRequest(title = "수정된 제목")

                every { userReader.getById(1L) } returns user
                every { postRepository.findActivePostById(1L) } returns post

                useCase.update(clubId, 1L, request, 1L)

                post.title shouldBe "수정된 제목"
                post.content shouldBe "원래 내용"
            }

            it("삭제된 Board 소속 Post를 수정하면 예외를 던진다") {
                every { postRepository.findActivePostById(1L) } returns null

                shouldThrow<PostNotFoundException> {
                    useCase.update(1L, 1L, UpdatePostRequest(title = "수정"), 1L)
                }
            }

            it("게시판이 ADMIN 전용으로 바뀐 후 일반 사용자가 수정하면 예외를 던진다") {
                val user = UserTestFixture.createActiveUser1(1L)
                val board =
                    BoardTestFixture.create(
                        name = "공지",
                        type = BoardType.NOTICE,
                        config = BoardConfig(writePermission = MemberRole.ADMIN),
                    )
                val clubId = board.club.id
                val post = PostTestFixture.create(title = "제목", content = "내용", user = user, board = board)

                every { userReader.getById(1L) } returns user
                every { postRepository.findActivePostById(1L) } returns post

                shouldThrow<CategoryAccessDeniedException> {
                    useCase.update(clubId, 1L, UpdatePostRequest(title = "수정"), 1L)
                }
            }

            it("게시판이 비공개로 바뀐 후 일반 사용자가 수정하면 예외를 던진다") {
                val user = UserTestFixture.createActiveUser1(1L)
                val board =
                    BoardTestFixture.create(
                        name = "비공개",
                        type = BoardType.GENERAL,
                        config = BoardConfig(isPrivate = true),
                    )
                val clubId = board.club.id
                val post = PostTestFixture.create(title = "제목", content = "내용", user = user, board = board)

                every { userReader.getById(1L) } returns user
                every { postRepository.findActivePostById(1L) } returns post

                shouldThrow<CategoryAccessDeniedException> {
                    useCase.update(clubId, 1L, UpdatePostRequest(title = "수정"), 1L)
                }
            }
        }

        describe("delete") {
            it("삭제 시 첨부 파일과 게시글을 soft delete한다") {
                val user = UserTestFixture.createActiveUser1(1L)
                val board = BoardTestFixture.create(name = "일반", type = BoardType.GENERAL)
                val clubId = board.club.id
                val post = PostTestFixture.create(title = "제목", content = "내용", user = user, board = board)
                val oldFile = createUploadedPostFile("old.png")

                every { userReader.getById(1L) } returns user
                every { postRepository.findActivePostById(1L) } returns post
                every { fileReader.findAll(FileOwnerType.POST, any<Long>(), any()) } returns listOf(oldFile)

                useCase.delete(clubId, 1L, 1L)

                oldFile.status.name shouldBe "DELETED"
                post.isDeleted shouldBe true
                verify(exactly = 0) { postRepository.delete(any()) }
            }

            it("삭제된 Board 소속 Post를 삭제하면 예외를 던진다") {
                every { postRepository.findActivePostById(1L) } returns null

                shouldThrow<PostNotFoundException> {
                    useCase.delete(1L, 1L, 1L)
                }
            }

            it("게시판이 ADMIN 전용으로 바뀐 후 일반 사용자가 삭제하면 예외를 던진다") {
                val user = UserTestFixture.createActiveUser1(1L)
                val board =
                    BoardTestFixture.create(
                        name = "공지",
                        type = BoardType.NOTICE,
                        config = BoardConfig(writePermission = MemberRole.ADMIN),
                    )
                val clubId = board.club.id
                val post = PostTestFixture.create(title = "제목", content = "내용", user = user, board = board)

                every { userReader.getById(1L) } returns user
                every { postRepository.findActivePostById(1L) } returns post

                shouldThrow<CategoryAccessDeniedException> {
                    useCase.delete(clubId, 1L, 1L)
                }
            }
        }

        describe("owner validation") {
            it("작성자가 아니면 수정 시 예외를 던진다") {
                val owner = UserTestFixture.createActiveUser1(1L)
                val otherUser = UserTestFixture.createActiveUser1(2L)
                val board = BoardTestFixture.create(name = "일반", type = BoardType.GENERAL)
                val clubId = board.club.id
                val post = PostTestFixture.create(title = "제목", content = "내용", user = owner, board = board)
                val request = UpdatePostRequest(title = "수정", content = "수정")

                every { userReader.getById(2L) } returns otherUser
                every { postRepository.findActivePostById(1L) } returns post

                shouldThrow<PostNotOwnedException> {
                    useCase.update(clubId, 1L, request, 2L)
                }
            }
        }
    })
