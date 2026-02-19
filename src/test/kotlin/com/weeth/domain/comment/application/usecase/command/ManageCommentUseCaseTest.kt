package com.weeth.domain.comment.application.usecase.command

import com.weeth.domain.board.domain.repository.PostRepository
import com.weeth.domain.board.fixture.PostTestFixture
import com.weeth.domain.comment.application.dto.request.CommentSaveRequest
import com.weeth.domain.comment.application.dto.request.CommentUpdateRequest
import com.weeth.domain.comment.application.exception.CommentAlreadyDeletedException
import com.weeth.domain.comment.application.exception.CommentNotFoundException
import com.weeth.domain.comment.application.exception.CommentNotOwnedException
import com.weeth.domain.comment.domain.entity.Comment
import com.weeth.domain.comment.domain.repository.CommentRepository
import com.weeth.domain.file.application.dto.request.FileSaveRequest
import com.weeth.domain.file.application.mapper.FileMapper
import com.weeth.domain.file.domain.entity.File
import com.weeth.domain.file.domain.entity.FileOwnerType
import com.weeth.domain.file.domain.entity.FileStatus
import com.weeth.domain.file.domain.repository.FileReader
import com.weeth.domain.file.domain.repository.FileRepository
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

class ManageCommentUseCaseTest :
    DescribeSpec({
        val commentRepository = mockk<CommentRepository>(relaxUnitFun = true)
        val postRepository = mockk<PostRepository>()
        val userGetService = mockk<UserGetService>()
        val fileReader = mockk<FileReader>()
        val fileRepository = mockk<FileRepository>(relaxed = true)
        val fileMapper = mockk<FileMapper>()

        val useCase =
            ManageCommentUseCase(
                commentRepository,
                postRepository,
                userGetService,
                fileReader,
                fileRepository,
                fileMapper,
            )

        beforeTest {
            clearMocks(commentRepository, postRepository, userGetService, fileReader, fileRepository, fileMapper)
            every { fileMapper.toFileList(any(), FileOwnerType.COMMENT, any()) } returns emptyList()
            every { commentRepository.save(any()) } answers { firstArg() }
            every { fileReader.findAll(FileOwnerType.COMMENT, any<Long>(), any<FileStatus>()) } returns emptyList()
            every { commentRepository.delete(any()) } just runs
        }

        describe("savePostComment") {
            it("최상위 댓글 저장 시 댓글 수가 증가한다") {
                val user = UserTestFixture.createActiveUser1(1L)
                val post = PostTestFixture.create(id = 10L, user = user)
                val dto = CommentSaveRequest(parentCommentId = null, content = "최상위 댓글", files = null)

                every { userGetService.find(1L) } returns user
                every { postRepository.findByIdWithLock(10L) } returns post

                useCase.savePostComment(dto, postId = 10L, userId = 1L)

                post.commentCount shouldBe 1
                verify(exactly = 1) { commentRepository.save(any()) }
                verify(exactly = 0) { commentRepository.findByIdAndPostId(any(), any()) }
            }

            it("부모 댓글이 존재하지 않으면 예외를 던진다") {
                val user = UserTestFixture.createActiveUser1(1L)
                val post = PostTestFixture.create(id = 10L, user = user)
                val dto = CommentSaveRequest(parentCommentId = 999L, content = "대댓글", files = null)

                every { userGetService.find(1L) } returns user
                every { postRepository.findByIdWithLock(10L) } returns post
                every { commentRepository.findByIdAndPostId(999L, 10L) } returns null

                shouldThrow<CommentNotFoundException> {
                    useCase.savePostComment(dto, postId = 10L, userId = 1L)
                }
            }
        }

        describe("updatePostComment") {
            it("작성자가 아니면 예외를 던진다") {
                val owner = UserTestFixture.createActiveUser1(1L)
                val post = PostTestFixture.create(id = 10L, user = owner)
                val comment = Comment(id = 200L, content = "old", post = post, user = owner)
                val dto = CommentUpdateRequest(content = "new", files = null)

                every { commentRepository.findByIdAndPostId(200L, 10L) } returns comment

                shouldThrow<CommentNotOwnedException> {
                    useCase.updatePostComment(dto, postId = 10L, commentId = 200L, userId = 2L)
                }
            }

            it("files가 있으면 기존 파일은 삭제되고 새 파일이 저장된다") {
                val owner = UserTestFixture.createActiveUser1(1L)
                val post = PostTestFixture.create(id = 10L, user = owner)
                val comment = Comment(id = 202L, content = "old", post = post, user = owner)
                val dto =
                    CommentUpdateRequest(
                        content = "new content",
                        files =
                            listOf(
                                FileSaveRequest(
                                    "new.png",
                                    "COMMENT/2026-02/123e4567-e89b-12d3-a456-426614174001_new.png",
                                    200L,
                                    "image/png",
                                ),
                            ),
                    )
                val oldFile =
                    File.createUploaded(
                        fileName = "old.png",
                        storageKey = "COMMENT/2026-02/123e4567-e89b-12d3-a456-426614174002_old.png",
                        fileSize = 200L,
                        contentType = "image/png",
                        ownerType = FileOwnerType.COMMENT,
                        ownerId = comment.id,
                    )
                val newFile =
                    File.createUploaded(
                        fileName = "new.png",
                        storageKey = "COMMENT/2026-02/123e4567-e89b-12d3-a456-426614174003_new.png",
                        fileSize = 200L,
                        contentType = "image/png",
                        ownerType = FileOwnerType.COMMENT,
                        ownerId = comment.id,
                    )

                every { commentRepository.findByIdAndPostId(202L, 10L) } returns comment
                every { fileReader.findAll(FileOwnerType.COMMENT, 202L, any()) } returns listOf(oldFile)
                every { fileMapper.toFileList(dto.files, FileOwnerType.COMMENT, 202L) } returns listOf(newFile)

                useCase.updatePostComment(dto, postId = 10L, commentId = 202L, userId = 1L)

                comment.content shouldBe "new content"
                oldFile.status.name shouldBe "DELETED"
                verify { fileRepository.saveAll(listOf(newFile)) }
            }
        }

        describe("deletePostComment") {
            it("리프 댓글 삭제 시 hard delete 되고 댓글 수가 감소한다") {
                val owner = UserTestFixture.createActiveUser1(1L)
                val post = PostTestFixture.create(id = 10L, user = owner, title = "title")
                post.commentCount = 1
                val comment = Comment(id = 310L, content = "leaf", post = post, user = owner)

                every { postRepository.findByIdWithLock(10L) } returns post
                every { commentRepository.findByIdAndPostId(310L, 10L) } returns comment

                useCase.deletePostComment(postId = 10L, commentId = 310L, userId = 1L)

                post.commentCount shouldBe 0
                verify(exactly = 1) { commentRepository.delete(comment) }
            }

            it("자식이 있는 댓글 삭제 시 soft delete 된다") {
                val owner = UserTestFixture.createActiveUser1(1L)
                val post = PostTestFixture.create(id = 10L, user = owner)
                post.commentCount = 2

                val comment = Comment(id = 300L, content = "target", post = post, user = owner)
                val child = Comment(id = 301L, content = "child", post = post, user = owner, parent = comment)
                comment.children.add(child)

                every { postRepository.findByIdWithLock(10L) } returns post
                every { commentRepository.findByIdAndPostId(300L, 10L) } returns comment

                useCase.deletePostComment(postId = 10L, commentId = 300L, userId = 1L)

                comment.isDeleted shouldBe true
                comment.content shouldBe "삭제된 댓글입니다."
                post.commentCount shouldBe 1
                verify(exactly = 0) { commentRepository.delete(comment) }
            }

            it("이미 삭제된 댓글은 삭제할 수 없다") {
                val owner = UserTestFixture.createActiveUser1(1L)
                val post = PostTestFixture.create(id = 10L, user = owner)
                val comment = Comment(id = 320L, content = "삭제된 댓글입니다.", post = post, user = owner, isDeleted = true)

                every { postRepository.findByIdWithLock(10L) } returns post
                every { commentRepository.findByIdAndPostId(320L, 10L) } returns comment

                shouldThrow<CommentAlreadyDeletedException> {
                    useCase.deletePostComment(postId = 10L, commentId = 320L, userId = 1L)
                }
            }
        }
    })
