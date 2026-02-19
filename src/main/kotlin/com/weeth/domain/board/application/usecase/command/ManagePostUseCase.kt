package com.weeth.domain.board.application.usecase.command

import com.weeth.domain.board.application.dto.request.CreatePostRequest
import com.weeth.domain.board.application.dto.request.UpdatePostRequest
import com.weeth.domain.board.application.dto.response.PostSaveResponse
import com.weeth.domain.board.application.exception.BoardNotFoundException
import com.weeth.domain.board.application.exception.CategoryAccessDeniedException
import com.weeth.domain.board.application.exception.PostNotFoundException
import com.weeth.domain.board.application.exception.PostNotOwnedException
import com.weeth.domain.board.application.mapper.PostMapper
import com.weeth.domain.board.domain.entity.Board
import com.weeth.domain.board.domain.entity.Post
import com.weeth.domain.board.domain.repository.BoardRepository
import com.weeth.domain.board.domain.repository.PostRepository
import com.weeth.domain.file.application.dto.request.FileSaveRequest
import com.weeth.domain.file.application.mapper.FileMapper
import com.weeth.domain.file.domain.entity.FileOwnerType
import com.weeth.domain.file.domain.repository.FileReader
import com.weeth.domain.file.domain.repository.FileRepository
import com.weeth.domain.user.domain.entity.User
import com.weeth.domain.user.domain.entity.enums.Role
import com.weeth.domain.user.domain.service.UserGetService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ManagePostUseCase(
    private val postRepository: PostRepository,
    private val boardRepository: BoardRepository, // 동일 도메인
    private val userGetService: UserGetService,
    private val fileRepository: FileRepository,
    private val fileReader: FileReader,
    private val fileMapper: FileMapper,
    private val postMapper: PostMapper,
) {
    @Transactional
    fun save(
        boardId: Long,
        request: CreatePostRequest,
        userId: Long,
    ): PostSaveResponse {
        val user = userGetService.find(userId) // todo: Reader 인터페이스로 수정
        val board = findBoard(boardId)
        checkWritePermission(board, user)

        val post =
            Post.create(
                title = request.title,
                content = request.content,
                user = user,
                board = board,
                cardinalNumber = request.cardinalNumber,
            )

        val savedPost = postRepository.save(post)
        savePostFiles(savedPost, request.files)
        return postMapper.toSaveResponse(savedPost)
    }

    @Transactional
    fun update(
        postId: Long,
        request: UpdatePostRequest,
        userId: Long,
    ): PostSaveResponse {
        val post = findPost(postId)
        validateOwner(post, userId)

        // TODO: PATCH 규칙 - title/content/cardinalNumber는 실제 변경된 경우에만 반영하도록 수정 필요
        post.update(
            newTitle = request.title,
            newContent = request.content,
            newCardinalNumber = request.cardinalNumber,
        )

        replacePostFiles(post, request.files)
        return postMapper.toSaveResponse(post)
    }

    @Transactional
    fun delete(
        postId: Long,
        userId: Long,
    ) {
        val post = findPost(postId)
        validateOwner(post, userId)

        markPostFilesDeleted(post.id)
        post.markDeleted()
    }

    private fun findBoard(boardId: Long): Board = boardRepository.findByIdAndIsDeletedFalse(boardId) ?: throw BoardNotFoundException()

    private fun findPost(postId: Long): Post = postRepository.findByIdAndIsDeletedFalse(postId) ?: throw PostNotFoundException()

    private fun validateOwner(
        post: Post,
        userId: Long,
    ) {
        if (!post.isOwnedBy(userId)) {
            throw PostNotOwnedException()
        }
    }

    private fun checkWritePermission(
        board: Board,
        user: User,
    ) {
        if (board.isAdminOnly && user.role != Role.ADMIN) {
            throw CategoryAccessDeniedException()
        }
    }

    private fun replacePostFiles(
        post: Post,
        files: List<FileSaveRequest>?,
    ) {
        if (files == null) {
            return
        }
        markPostFilesDeleted(post.id)
        savePostFiles(post, files)
    }

    private fun savePostFiles(
        post: Post,
        files: List<FileSaveRequest>?,
    ) {
        val mappedFiles = fileMapper.toFileList(files, FileOwnerType.POST, post.id)
        if (mappedFiles.isNotEmpty()) {
            fileRepository.saveAll(mappedFiles)
        }
    }

    private fun markPostFilesDeleted(postId: Long) {
        fileReader.findAll(FileOwnerType.POST, postId).forEach { it.markDeleted() }
    }
}
