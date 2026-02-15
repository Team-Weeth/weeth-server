package com.weeth.domain.comment.application.usecase

import com.weeth.domain.board.domain.service.NoticeFindService
import com.weeth.domain.board.fixture.NoticeTestFixture
import com.weeth.domain.comment.application.dto.CommentDTO
import com.weeth.domain.comment.application.mapper.CommentMapper
import com.weeth.domain.comment.domain.service.CommentDeleteService
import com.weeth.domain.comment.domain.service.CommentFindService
import com.weeth.domain.comment.domain.service.CommentSaveService
import com.weeth.domain.comment.fixture.CommentTestFixture
import com.weeth.domain.file.application.mapper.FileMapper
import com.weeth.domain.file.domain.service.FileDeleteService
import com.weeth.domain.file.domain.service.FileGetService
import com.weeth.domain.file.domain.service.FileSaveService
import com.weeth.domain.user.application.exception.UserNotMatchException
import com.weeth.domain.user.domain.service.UserGetService
import com.weeth.domain.user.fixture.UserTestFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class NoticeCommentUsecaseImplTest :
    DescribeSpec({

        val commentSaveService = mockk<CommentSaveService>(relaxUnitFun = true)
        val commentFindService = mockk<CommentFindService>()
        val commentDeleteService = mockk<CommentDeleteService>(relaxUnitFun = true)
        val fileSaveService = mockk<FileSaveService>(relaxUnitFun = true)
        val fileGetService = mockk<FileGetService>()
        val fileDeleteService = mockk<FileDeleteService>(relaxUnitFun = true)
        val fileMapper = mockk<FileMapper>()
        val noticeFindService = mockk<NoticeFindService>()
        val userGetService = mockk<UserGetService>()
        val commentMapper = mockk<CommentMapper>()

        val noticeCommentUsecase =
            NoticeCommentUsecaseImpl(
                commentSaveService,
                commentFindService,
                commentDeleteService,
                fileSaveService,
                fileGetService,
                fileDeleteService,
                fileMapper,
                noticeFindService,
                userGetService,
                commentMapper,
            )

        describe("saveNoticeComment") {
            it("부모 댓글이 없는 공지사항 댓글 작성") {
                val userId = 1L
                val noticeId = 1L
                val commentId = 1L

                val user = UserTestFixture.createActiveUser1(1L)
                val notice = NoticeTestFixture.createNotice(id = noticeId, title = "제목1")

                val dto = CommentDTO.Save(null, "댓글1", listOf())

                val comment = CommentTestFixture.createComment(commentId, dto.content(), user, notice)

                every { commentMapper.fromCommentDto(dto, notice, user, null) } returns comment
                every { userGetService.find(user.id) } returns user
                every { noticeFindService.find(notice.id) } returns notice
                every { fileMapper.toFileList(dto.files(), comment) } returns listOf()

                noticeCommentUsecase.saveNoticeComment(dto, noticeId, userId)

                verify { userGetService.find(userId) }
                verify { noticeFindService.find(noticeId) }
                verify { commentSaveService.save(comment) }

                notice.comments shouldContain comment
            }

            it("부모 댓글이 있는 경우 공지사항 댓글 작성") {
                val userId = 1L
                val noticeId = 1L
                val parentCommentId = 1L
                val childCommentId = 2L

                val user = UserTestFixture.createActiveUser1(parentCommentId)
                val notice = NoticeTestFixture.createNotice(id = noticeId, title = "제목1")

                val parentComment = CommentTestFixture.createComment(parentCommentId, "부모 댓글", user, notice)

                val childCommentDTO = CommentDTO.Save(parentCommentId, "자식 댓글", listOf())
                val childComment = CommentTestFixture.createComment(childCommentId, childCommentDTO.content(), user, notice)

                every { commentMapper.fromCommentDto(childCommentDTO, notice, user, parentComment) } returns childComment
                every { userGetService.find(user.id) } returns user
                every { commentFindService.find(parentComment.id) } returns parentComment
                every { noticeFindService.find(notice.id) } returns notice
                every { fileMapper.toFileList(childCommentDTO.files(), childComment) } returns listOf()

                noticeCommentUsecase.saveNoticeComment(childCommentDTO, noticeId, userId)

                verify { commentFindService.find(parentComment.id) }
                verify { commentSaveService.save(childComment) }

                parentComment.children shouldContain childComment
            }
        }

        describe("updateNoticeComment") {
            it("공지사항 댓글 수정 시 작성자와 수정 요청자가 다르면 예외가 발생한다") {
                val different = 2L
                val noticeId = 1L
                val commentId = 1L

                val user = UserTestFixture.createActiveUser1(1L)
                val user2 = UserTestFixture.createActiveUser1(2L)
                val notice = NoticeTestFixture.createNotice(id = noticeId, title = "제목1")

                val dto = CommentDTO.Update("수정 완료", listOf())
                val comment = CommentTestFixture.createComment(commentId, dto.content(), user, notice)

                every { userGetService.find(user2.id) } returns user2
                every { noticeFindService.find(notice.id) } returns notice
                every { commentFindService.find(comment.id) } returns comment

                shouldThrow<UserNotMatchException> {
                    noticeCommentUsecase.updateNoticeComment(dto, noticeId, comment.id, different)
                }
            }
        }
    })
