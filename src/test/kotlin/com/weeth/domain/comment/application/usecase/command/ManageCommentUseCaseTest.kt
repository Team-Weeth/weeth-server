package com.weeth.domain.comment.application.usecase.command

import com.weeth.domain.board.domain.entity.enums.Category
import com.weeth.domain.board.domain.repository.NoticeRepository
import com.weeth.domain.board.domain.repository.PostRepository
import com.weeth.domain.board.fixture.NoticeTestFixture
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
import io.mockk.mockk
import io.mockk.verify
import org.springframework.test.util.ReflectionTestUtils

class ManageCommentUseCaseTest :
    DescribeSpec({
        val commentRepository = mockk<CommentRepository>(relaxUnitFun = true)
        val postRepository = mockk<PostRepository>()
        val noticeRepository = mockk<NoticeRepository>()
        val userGetService = mockk<UserGetService>()
        val fileReader = mockk<FileReader>()
        val fileRepository = mockk<FileRepository>(relaxed = true)
        val fileMapper = mockk<FileMapper>()

        val useCase =
            ManageCommentUseCase(
                commentRepository,
                postRepository,
                noticeRepository,
                userGetService,
                fileReader,
                fileRepository,
                fileMapper,
            )

        beforeTest {
            clearMocks(
                commentRepository,
                postRepository,
                noticeRepository,
                userGetService,
                fileReader,
                fileRepository,
                fileMapper,
            )
            every { fileMapper.toFileList(any(), FileOwnerType.COMMENT, any()) } returns emptyList()
            every { commentRepository.save(any()) } answers { firstArg() }
            every { fileReader.findAll(FileOwnerType.COMMENT, any<Long>(), any<FileStatus>()) } returns emptyList()
        }

        describe("savePostComment") {
            it("최상위 댓글 저장 성공 시 댓글 수를 증가시킨다") {
                val user = UserTestFixture.createActiveUser1(1L)
                val post = PostTestFixture.createPost(id = 10L, title = "title", category = Category.StudyLog)
                val dto = CommentSaveRequest(parentCommentId = null, content = "최상위 댓글", files = null)

                every { userGetService.find(1L) } returns user
                every { postRepository.findByIdWithLock(10L) } returns post

                useCase.savePostComment(dto, postId = 10L, userId = 1L)

                post.commentCount shouldBe 1
                verify { commentRepository.save(any()) }
                verify(exactly = 0) { commentRepository.findByIdAndPostId(any(), any()) }
            }

            it("대댓글 저장 성공 시 같은 게시글 경계를 검증하고 댓글 수를 증가시킨다") {
                val user = UserTestFixture.createActiveUser1(1L)
                val post = PostTestFixture.createPost(id = 10L, title = "title", category = Category.StudyLog)
                val parent = Comment(id = 100L, content = "parent", post = post, user = user)
                val dto =
                    CommentSaveRequest(
                        parentCommentId = 100L,
                        content = "child",
                        files = listOf(FileSaveRequest("f.png", "COMMENT/2026-02/123e4567-e89b-12d3-a456-426614174000_f.png", 100L, "image/png")),
                    )
                val mappedFiles =
                    listOf(
                        File.createUploaded(
                            fileName = "f.png",
                            storageKey = "COMMENT/2026-02/123e4567-e89b-12d3-a456-426614174000_f.png",
                            fileSize = 100L,
                            contentType = "image/png",
                            ownerType = FileOwnerType.COMMENT,
                            ownerId = 999L,
                        ),
                    )

                every { userGetService.find(1L) } returns user
                every { postRepository.findByIdWithLock(10L) } returns post
                every { commentRepository.findByIdAndPostId(100L, 10L) } returns parent
                every { fileMapper.toFileList(dto.files, FileOwnerType.COMMENT, any()) } returns mappedFiles

                useCase.savePostComment(dto, postId = 10L, userId = 1L)

                post.commentCount shouldBe 1
                verify { commentRepository.findByIdAndPostId(100L, 10L) }
                verify { commentRepository.save(any()) }
                verify { fileRepository.saveAll(mappedFiles) }
            }

            it("부모 댓글이 다른 리소스면 예외를 던진다") {
                val user = UserTestFixture.createActiveUser1(1L)
                val post = PostTestFixture.createPost(id = 10L, title = "title", category = Category.StudyLog)
                val dto = CommentSaveRequest(parentCommentId = 999L, content = "child", files = null)

                every { userGetService.find(1L) } returns user
                every { postRepository.findByIdWithLock(10L) } returns post
                every { commentRepository.findByIdAndPostId(999L, 10L) } returns null

                shouldThrow<CommentNotFoundException> {
                    useCase.savePostComment(dto, postId = 10L, userId = 1L)
                }

                verify(exactly = 0) { commentRepository.save(any()) }
            }
        }

        describe("updatePostComment") {
            it("작성자가 아니면 예외를 던진다") {
                val owner = UserTestFixture.createActiveUser1(1L)
                val post = PostTestFixture.createPost(id = 10L, title = "title", category = Category.StudyLog)
                val comment = Comment(id = 200L, content = "old", post = post, user = owner)
                val dto = CommentUpdateRequest(content = "new", files = null)

                every { commentRepository.findByIdAndPostId(200L, 10L) } returns comment

                shouldThrow<CommentNotOwnedException> {
                    useCase.updatePostComment(dto, postId = 10L, commentId = 200L, userId = 2L)
                }

                verify(exactly = 0) { fileRepository.saveAll(any<List<File>>()) }
            }

            it("files가 null이면 기존 첨부를 유지한다") {
                val owner = UserTestFixture.createActiveUser1(1L)
                val post = PostTestFixture.createPost(id = 10L, title = "title", category = Category.StudyLog)
                val comment = Comment(id = 201L, content = "old", post = post, user = owner)
                val dto = CommentUpdateRequest(content = "new content", files = null)

                every { commentRepository.findByIdAndPostId(201L, 10L) } returns comment

                useCase.updatePostComment(dto, postId = 10L, commentId = 201L, userId = 1L)

                comment.content shouldBe "new content"
                verify(exactly = 0) { fileReader.findAll(FileOwnerType.COMMENT, any<Long>(), any<FileStatus>()) }
                verify(exactly = 0) { fileRepository.saveAll(any<List<File>>()) }
            }

            it("files가 있으면 기존 파일을 삭제하고 새 파일을 저장한다") {
                val owner = UserTestFixture.createActiveUser1(1L)
                val post = PostTestFixture.createPost(id = 10L, title = "title", category = Category.StudyLog)
                val comment = Comment(id = 202L, content = "old", post = post, user = owner)
                val dto = CommentUpdateRequest(
                    content = "new content",
                    files = listOf(FileSaveRequest("new.png", "COMMENT/2026-02/123e4567-e89b-12d3-a456-426614174001_new.png", 200L, "image/png")),
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

            it("files가 빈 배열이면 기존 파일을 전체 삭제하고 새 파일은 저장하지 않는다") {
                val owner = UserTestFixture.createActiveUser1(1L)
                val post = PostTestFixture.createPost(id = 10L, title = "title", category = Category.StudyLog)
                val comment = Comment(id = 204L, content = "old", post = post, user = owner)
                val dto = CommentUpdateRequest(content = "new content", files = emptyList())
                val oldFile =
                    File.createUploaded(
                        fileName = "old.png",
                        storageKey = "COMMENT/2026-02/123e4567-e89b-12d3-a456-426614174004_old2.png",
                        fileSize = 300L,
                        contentType = "image/png",
                        ownerType = FileOwnerType.COMMENT,
                        ownerId = comment.id,
                    )

                every { commentRepository.findByIdAndPostId(204L, 10L) } returns comment
                every { fileReader.findAll(FileOwnerType.COMMENT, 204L, any()) } returns listOf(oldFile)

                useCase.updatePostComment(dto, postId = 10L, commentId = 204L, userId = 1L)

                oldFile.status.name shouldBe "DELETED"
                verify(exactly = 0) { fileRepository.saveAll(any<List<File>>()) }
            }

            it("삭제된 댓글은 수정할 수 없다") {
                val owner = UserTestFixture.createActiveUser1(1L)
                val post = PostTestFixture.createPost(id = 10L, title = "title", category = Category.StudyLog)
                val comment = Comment(id = 203L, content = "삭제된 댓글입니다.", post = post, user = owner, isDeleted = true)
                val dto = CommentUpdateRequest(content = "new content", files = null)

                every { commentRepository.findByIdAndPostId(203L, 10L) } returns comment

                shouldThrow<CommentAlreadyDeletedException> {
                    useCase.updatePostComment(dto, postId = 10L, commentId = 203L, userId = 1L)
                }

                verify(exactly = 0) { fileReader.findAll(FileOwnerType.COMMENT, any<Long>(), any<FileStatus>()) }
                verify(exactly = 0) { fileRepository.saveAll(any<List<File>>()) }
            }
        }

        describe("deletePostComment") {
            it("자식이 있는 댓글 삭제 시 soft delete 하고 댓글 수를 감소시킨다") {
                val owner = UserTestFixture.createActiveUser1(1L)
                val post = PostTestFixture.createPost(id = 10L, title = "title", category = Category.StudyLog)
                ReflectionTestUtils.setField(post, "commentCount", 2)

                val comment = Comment(id = 300L, content = "target", post = post, user = owner)
                val child =
                    Comment(id = 301L, content = "child", post = post, user = owner, parent = comment)
                comment.children.add(child)

                every { postRepository.findByIdWithLock(10L) } returns post
                every { commentRepository.findByIdAndPostId(300L, 10L) } returns comment

                useCase.deletePostComment(postId = 10L, commentId = 300L, userId = 1L)

                comment.isDeleted shouldBe true
                comment.content shouldBe "삭제된 댓글입니다."
                post.commentCount shouldBe 1
                verify(exactly = 0) { commentRepository.delete(comment) }
            }

            it("이미 삭제된 댓글을 다시 삭제하면 예외를 던진다") {
                val owner = UserTestFixture.createActiveUser1(1L)
                val post = PostTestFixture.createPost(id = 10L, title = "title", category = Category.StudyLog)
                ReflectionTestUtils.setField(post, "commentCount", 2)

                val comment =
                    Comment(id = 300L, content = "target", post = post, user = owner, isDeleted = true)
                val child =
                    Comment(id = 301L, content = "child", post = post, user = owner, parent = comment)
                comment.children.add(child)

                every { postRepository.findByIdWithLock(10L) } returns post
                every { commentRepository.findByIdAndPostId(300L, 10L) } returns comment

                shouldThrow<CommentAlreadyDeletedException> {
                    useCase.deletePostComment(postId = 10L, commentId = 300L, userId = 1L)
                }

                post.commentCount shouldBe 2
            }

            it("이미 삭제된 댓글은 자식이 없어도 예외를 던진다") {
                val owner = UserTestFixture.createActiveUser1(1L)
                val post = PostTestFixture.createPost(id = 10L, title = "title", category = Category.StudyLog)
                val comment = Comment(id = 300L, content = "삭제된 댓글입니다.", post = post, user = owner, isDeleted = true)

                every { postRepository.findByIdWithLock(10L) } returns post
                every { commentRepository.findByIdAndPostId(300L, 10L) } returns comment

                shouldThrow<CommentAlreadyDeletedException> {
                    useCase.deletePostComment(postId = 10L, commentId = 300L, userId = 1L)
                }

                verify(exactly = 0) { commentRepository.delete(any()) }
            }

            it("자식 없는 리프 댓글 삭제 시 hard delete하고 댓글 수를 감소시킨다") {
                val owner = UserTestFixture.createActiveUser1(1L)
                val post = PostTestFixture.createPost(id = 10L, title = "title", category = Category.StudyLog)
                ReflectionTestUtils.setField(post, "commentCount", 1)
                val comment = Comment(id = 310L, content = "리프", post = post, user = owner)

                every { postRepository.findByIdWithLock(10L) } returns post
                every { commentRepository.findByIdAndPostId(310L, 10L) } returns comment

                useCase.deletePostComment(postId = 10L, commentId = 310L, userId = 1L)

                post.commentCount shouldBe 0
                verify { commentRepository.delete(comment) }
            }

            it("부모가 삭제됐어도 자식이 2개 이상이면 부모를 삭제하지 않는다") {
                val owner = UserTestFixture.createActiveUser1(1L)
                val post = PostTestFixture.createPost(id = 10L, title = "title", category = Category.StudyLog)
                ReflectionTestUtils.setField(post, "commentCount", 2)

                val parent =
                    Comment(
                        id = 400L,
                        content = "삭제된 댓글입니다.",
                        post = post,
                        user = owner,
                        isDeleted = true,
                    )
                val child1 = Comment(id = 401L, content = "첫째", post = post, user = owner, parent = parent)
                val child2 = Comment(id = 402L, content = "둘째", post = post, user = owner, parent = parent)
                parent.children.add(child1)
                parent.children.add(child2)

                every { postRepository.findByIdWithLock(10L) } returns post
                every { commentRepository.findByIdAndPostId(401L, 10L) } returns child1

                useCase.deletePostComment(postId = 10L, commentId = 401L, userId = 1L)

                verify { commentRepository.delete(child1) }
                verify(exactly = 0) { commentRepository.delete(parent) }
            }

            it("리프 댓글 삭제 시 부모가 삭제 상태이고 마지막 자식이면 부모까지 물리 삭제한다") {
                val owner = UserTestFixture.createActiveUser1(1L)
                val post = PostTestFixture.createPost(id = 10L, title = "title", category = Category.StudyLog)
                ReflectionTestUtils.setField(post, "commentCount", 1)

                val parent =
                    Comment(
                        id = 400L,
                        content = "삭제된 댓글입니다.",
                        post = post,
                        user = owner,
                        isDeleted = true,
                    )
                val child = Comment(id = 401L, content = "leaf", post = post, user = owner, parent = parent)
                parent.children.add(child)

                every { postRepository.findByIdWithLock(10L) } returns post
                every { commentRepository.findByIdAndPostId(401L, 10L) } returns child
                val childFile =
                    File.createUploaded(
                        fileName = "a",
                        storageKey = "COMMENT/2026-02/123e4567-e89b-12d3-a456-426614174005_a.png",
                        fileSize = 100L,
                        contentType = "image/png",
                        ownerType = FileOwnerType.COMMENT,
                        ownerId = 401L,
                    )
                every { fileReader.findAll(FileOwnerType.COMMENT, 401L, any()) } returns
                    listOf(
                        childFile,
                    )
                every { fileReader.findAll(FileOwnerType.COMMENT, 400L, any()) } returns emptyList()

                useCase.deletePostComment(postId = 10L, commentId = 401L, userId = 1L)

                post.commentCount shouldBe 0
                childFile.status.name shouldBe "DELETED"
                verify { commentRepository.delete(child) }
                verify { commentRepository.delete(parent) }
            }
        }

        describe("saveNoticeComment") {
            it("공지 댓글 생성도 동일하게 lock 기반으로 처리한다") {
                val user = UserTestFixture.createActiveUser1(1L)
                val notice = NoticeTestFixture.createNotice(id = 11L, title = "notice", user = user)
                val dto = CommentSaveRequest(parentCommentId = null, content = "notice comment", files = null)

                every { userGetService.find(1L) } returns user
                every { noticeRepository.findByIdWithLock(11L) } returns notice

                useCase.saveNoticeComment(dto, noticeId = 11L, userId = 1L)

                notice.commentCount shouldBe 1
                verify { noticeRepository.findByIdWithLock(11L) }
                verify { commentRepository.save(any()) }
            }
        }

        describe("updateNoticeComment") {
            it("작성자가 아니면 예외를 던진다") {
                val owner = UserTestFixture.createActiveUser1(1L)
                val notice = NoticeTestFixture.createNotice(id = 11L, title = "notice", user = owner)
                val comment = Comment(id = 500L, content = "old", notice = notice, user = owner)
                val dto = CommentUpdateRequest(content = "new", files = null)

                every { commentRepository.findByIdAndNoticeId(500L, 11L) } returns comment

                shouldThrow<CommentNotOwnedException> {
                    useCase.updateNoticeComment(dto, noticeId = 11L, commentId = 500L, userId = 2L)
                }
            }

            it("작성자이면 내용을 변경한다") {
                val owner = UserTestFixture.createActiveUser1(1L)
                val notice = NoticeTestFixture.createNotice(id = 11L, title = "notice", user = owner)
                val comment = Comment(id = 501L, content = "old", notice = notice, user = owner)
                val dto = CommentUpdateRequest(content = "updated", files = null)

                every { commentRepository.findByIdAndNoticeId(501L, 11L) } returns comment

                useCase.updateNoticeComment(dto, noticeId = 11L, commentId = 501L, userId = 1L)

                comment.content shouldBe "updated"
            }

            it("삭제된 댓글은 수정할 수 없다") {
                val owner = UserTestFixture.createActiveUser1(1L)
                val notice = NoticeTestFixture.createNotice(id = 11L, title = "notice", user = owner)
                val comment = Comment(id = 502L, content = "삭제된 댓글입니다.", notice = notice, user = owner, isDeleted = true)
                val dto = CommentUpdateRequest(content = "updated", files = null)

                every { commentRepository.findByIdAndNoticeId(502L, 11L) } returns comment

                shouldThrow<CommentAlreadyDeletedException> {
                    useCase.updateNoticeComment(dto, noticeId = 11L, commentId = 502L, userId = 1L)
                }

                verify(exactly = 0) { fileReader.findAll(FileOwnerType.COMMENT, any<Long>(), any<FileStatus>()) }
                verify(exactly = 0) { fileRepository.saveAll(any<List<File>>()) }
            }

            it("files가 빈 배열이면 기존 파일을 전체 삭제하고 새 파일은 저장하지 않는다") {
                val owner = UserTestFixture.createActiveUser1(1L)
                val notice = NoticeTestFixture.createNotice(id = 11L, title = "notice", user = owner)
                val comment = Comment(id = 503L, content = "old", notice = notice, user = owner)
                val dto = CommentUpdateRequest(content = "updated", files = emptyList())
                val oldFile =
                    File.createUploaded(
                        fileName = "old.png",
                        storageKey = "COMMENT/2026-02/123e4567-e89b-12d3-a456-426614174006_old3.png",
                        fileSize = 400L,
                        contentType = "image/png",
                        ownerType = FileOwnerType.COMMENT,
                        ownerId = comment.id,
                    )

                every { commentRepository.findByIdAndNoticeId(503L, 11L) } returns comment
                every { fileReader.findAll(FileOwnerType.COMMENT, 503L, any()) } returns listOf(oldFile)

                useCase.updateNoticeComment(dto, noticeId = 11L, commentId = 503L, userId = 1L)

                oldFile.status.name shouldBe "DELETED"
                verify(exactly = 0) { fileRepository.saveAll(any<List<File>>()) }
            }
        }

        describe("deleteNoticeComment") {
            it("자식 없는 리프 댓글 삭제 시 hard delete하고 댓글 수를 감소시킨다") {
                val owner = UserTestFixture.createActiveUser1(1L)
                val notice = NoticeTestFixture.createNotice(id = 11L, title = "notice", user = owner)
                ReflectionTestUtils.setField(notice, "commentCount", 1)
                val comment = Comment(id = 600L, content = "리프", notice = notice, user = owner)

                every { noticeRepository.findByIdWithLock(11L) } returns notice
                every { commentRepository.findByIdAndNoticeId(600L, 11L) } returns comment

                useCase.deleteNoticeComment(noticeId = 11L, commentId = 600L, userId = 1L)

                notice.commentCount shouldBe 0
                verify { commentRepository.delete(comment) }
            }

            it("작성자가 아니면 예외를 던진다") {
                val owner = UserTestFixture.createActiveUser1(1L)
                val notice = NoticeTestFixture.createNotice(id = 11L, title = "notice", user = owner)
                val comment = Comment(id = 601L, content = "리프", notice = notice, user = owner)

                every { noticeRepository.findByIdWithLock(11L) } returns notice
                every { commentRepository.findByIdAndNoticeId(601L, 11L) } returns comment

                shouldThrow<CommentNotOwnedException> {
                    useCase.deleteNoticeComment(noticeId = 11L, commentId = 601L, userId = 2L)
                }

                verify(exactly = 0) { commentRepository.delete(any()) }
            }
        }
    })
