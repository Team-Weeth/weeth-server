package com.weeth.domain.comment.application.usecase.command

import com.weeth.domain.board.application.exception.NoticeNotFoundException
import com.weeth.domain.board.application.exception.PostNotFoundException
import com.weeth.domain.board.domain.entity.Notice
import com.weeth.domain.board.domain.entity.Post
import com.weeth.domain.board.domain.repository.NoticeRepository
import com.weeth.domain.board.domain.repository.PostRepository
import com.weeth.domain.comment.application.dto.request.CommentSaveRequest
import com.weeth.domain.comment.application.dto.request.CommentUpdateRequest
import com.weeth.domain.comment.application.exception.CommentAlreadyDeletedException
import com.weeth.domain.comment.application.exception.CommentNotFoundException
import com.weeth.domain.comment.application.exception.CommentNotOwnedException
import com.weeth.domain.comment.domain.entity.Comment
import com.weeth.domain.comment.domain.repository.CommentRepository
import com.weeth.domain.file.application.dto.request.FileSaveRequest
import com.weeth.domain.file.application.mapper.FileMapper
import com.weeth.domain.file.domain.entity.FileOwnerType
import com.weeth.domain.file.domain.repository.FileReader
import com.weeth.domain.file.domain.repository.FileRepository
import com.weeth.domain.user.domain.service.UserGetService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ManageCommentUseCase(
    private val commentRepository: CommentRepository,
    private val postRepository: PostRepository,
    private val noticeRepository: NoticeRepository,
    private val userGetService: UserGetService,
    private val fileReader: FileReader,
    private val fileRepository: FileRepository,
    private val fileMapper: FileMapper,
) : PostCommentUsecase,
    NoticeCommentUsecase {
    // Todo: Board 도메인 리팩토링 후 단일 Post 대응으로 수정.
    @Transactional
    override fun savePostComment(
        dto: CommentSaveRequest,
        postId: Long,
        userId: Long,
    ) {
        val user = userGetService.find(userId)
        val post = findPostWithLock(postId)
        val parent =
            dto.parentCommentId?.let { parentId ->
                commentRepository.findByIdAndPostId(parentId, postId) ?: throw CommentNotFoundException()
            }

        val comment =
            Comment.createForPost(
                content = dto.content,
                post = post,
                user = user,
                parent = parent,
            )
        val savedComment = commentRepository.save(comment)
        saveCommentFiles(savedComment, dto.files)
        post.increaseCommentCount()
    }

    @Transactional
    override fun updatePostComment(
        dto: CommentUpdateRequest,
        postId: Long,
        commentId: Long,
        userId: Long,
    ) {
        val comment = commentRepository.findByIdAndPostId(commentId, postId) ?: throw CommentNotFoundException()
        ensureOwner(comment, userId)
        ensureNotDeleted(comment)

        comment.updateContent(dto.content)
        replaceCommentFiles(comment, dto.files)
    }

    @Transactional
    override fun deletePostComment(
        postId: Long,
        commentId: Long,
        userId: Long,
    ) {
        val post = findPostWithLock(postId)
        val comment = commentRepository.findByIdAndPostId(commentId, postId) ?: throw CommentNotFoundException()
        ensureOwner(comment, userId)

        deleteComment(comment)
        post.decreaseCommentCount()
    }

    @Transactional
    override fun saveNoticeComment(
        dto: CommentSaveRequest,
        noticeId: Long,
        userId: Long,
    ) {
        val user = userGetService.find(userId)
        val notice = findNoticeWithLock(noticeId)
        val parent =
            dto.parentCommentId?.let { parentId ->
                commentRepository.findByIdAndNoticeId(parentId, noticeId) ?: throw CommentNotFoundException()
            }

        val comment =
            Comment.createForNotice(
                content = dto.content,
                notice = notice,
                user = user,
                parent = parent,
            )
        val savedComment = commentRepository.save(comment)
        saveCommentFiles(savedComment, dto.files)
        notice.increaseCommentCount()
    }

    @Transactional
    override fun updateNoticeComment(
        dto: CommentUpdateRequest,
        noticeId: Long,
        commentId: Long,
        userId: Long,
    ) {
        val comment = commentRepository.findByIdAndNoticeId(commentId, noticeId) ?: throw CommentNotFoundException()
        ensureOwner(comment, userId)
        ensureNotDeleted(comment)

        comment.updateContent(dto.content)
        replaceCommentFiles(comment, dto.files)
    }

    @Transactional
    override fun deleteNoticeComment(
        noticeId: Long,
        commentId: Long,
        userId: Long,
    ) {
        val notice = findNoticeWithLock(noticeId)
        val comment =
            commentRepository.findByIdAndNoticeId(commentId, noticeId) ?: throw CommentNotFoundException()
        ensureOwner(comment, userId)

        deleteComment(comment)
        notice.decreaseCommentCount()
    }

    private fun saveCommentFiles(
        comment: Comment,
        files: List<FileSaveRequest>?,
    ) {
        val mappedFiles = fileMapper.toFileList(files, FileOwnerType.COMMENT, comment.id)
        if (mappedFiles.isNotEmpty()) {
            fileRepository.saveAll(mappedFiles)
        }
    }

    private fun replaceCommentFiles(
        comment: Comment,
        files: List<FileSaveRequest>?,
    ) {
        // 계약:
        // files == null -> 첨부 유지(변경 안 함)
        // files == []   -> 기존 첨부 전체 삭제
        // files == [...] -> 기존 첨부 삭제 후 전달 목록으로 교체
        if (files == null) {
            return
        }

        markCommentFilesDeleted(comment.id)
        saveCommentFiles(comment, files)
    }

    private fun deleteComment(comment: Comment) {
        if (comment.isDeleted) {
            throw CommentAlreadyDeletedException()
        }

        // 자식 댓글이 없는 경우 -> 삭제
        if (comment.children.isEmpty()) {
            deleteCommentFiles(comment)
            val parent = comment.parent
            val shouldDeleteParent = parent?.let { it.isDeleted && it.children.size == 1 } == true
            commentRepository.delete(comment)

            // 부모 댓글이 삭제된 상태이고 유일한 자식이었던 경우 -> 부모 댓글도 삭제
            if (shouldDeleteParent) {
                parent.let {
                    deleteCommentFiles(it)
                    commentRepository.delete(it)
                }
            }
            return
        }

        // 자식 댓글이 있는 경우 -> 댓글을 Soft Delete해 서비스에서 "삭제된 댓글"으로 표시
        deleteCommentFiles(comment)
        comment.markAsDeleted()
    }

    private fun deleteCommentFiles(comment: Comment) {
        markCommentFilesDeleted(comment.id)
    }

    private fun markCommentFilesDeleted(commentId: Long) {
        fileReader
            .findAll(FileOwnerType.COMMENT, commentId)
            .forEach { it.markDeleted() }
    }

    private fun ensureOwner(
        comment: Comment,
        userId: Long,
    ) {
        if (!comment.isOwnedBy(userId)) {
            throw CommentNotOwnedException()
        }
    }

    private fun ensureNotDeleted(comment: Comment) {
        if (comment.isDeleted) {
            throw CommentAlreadyDeletedException()
        }
    }

    private fun findPostWithLock(postId: Long): Post = postRepository.findByIdWithLock(postId) ?: throw PostNotFoundException()

    private fun findNoticeWithLock(noticeId: Long): Notice = noticeRepository.findByIdWithLock(noticeId) ?: throw NoticeNotFoundException()
}
