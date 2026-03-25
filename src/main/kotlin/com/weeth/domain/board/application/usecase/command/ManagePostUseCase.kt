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
import com.weeth.domain.cardinal.domain.repository.CardinalReader
import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.file.application.dto.request.FileSaveRequest
import com.weeth.domain.file.application.mapper.FileMapper
import com.weeth.domain.file.domain.enums.FileOwnerType
import com.weeth.domain.file.domain.repository.FileReader
import com.weeth.domain.file.domain.repository.FileRepository
import com.weeth.domain.user.domain.repository.UserReader
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ManagePostUseCase(
    private val postRepository: PostRepository,
    private val boardRepository: BoardRepository,
    private val userReader: UserReader,
    private val clubMemberPolicy: ClubMemberPolicy,
    private val cardinalReader: CardinalReader,
    private val fileRepository: FileRepository,
    private val fileReader: FileReader,
    private val fileMapper: FileMapper,
    private val postMapper: PostMapper,
) {
    @Transactional
    fun save(
        clubId: Long,
        boardId: Long,
        request: CreatePostRequest,
        userId: Long,
    ): PostSaveResponse {
        val member = clubMemberPolicy.getActiveMember(clubId, userId)
        val user = userReader.getById(userId)
        val board = findBoardInClub(boardId, clubId)
        validateWritePermission(board, member)

        val currentCardinalNumber = cardinalReader.findInProgressByClubId(clubId)?.cardinalNumber
        val post =
            Post.create(
                title = request.title,
                content = request.content,
                user = user,
                board = board,
                cardinalNumber = currentCardinalNumber,
            )

        val savedPost = postRepository.save(post)
        savePostFiles(savedPost, request.files)
        return postMapper.toSaveResponse(savedPost)
    }

    @Transactional
    fun update(
        clubId: Long,
        postId: Long,
        request: UpdatePostRequest,
        userId: Long,
    ): PostSaveResponse {
        val member = clubMemberPolicy.getActiveMember(clubId, userId)
        val user = userReader.getById(userId)
        val post = findPost(postId)
        if (post.board.club.id != clubId) throw PostNotFoundException()
        validateOwner(post, userId)
        validateWritePermission(post.board, member)

        post.update(
            newTitle = request.title,
            newContent = request.content,
        )

        replacePostFiles(post, request.files)
        return postMapper.toSaveResponse(post)
    }

    @Transactional
    fun delete(
        clubId: Long,
        postId: Long,
        userId: Long,
    ) {
        val member = clubMemberPolicy.getActiveMember(clubId, userId)
        val user = userReader.getById(userId)
        val post = findPost(postId)
        if (post.board.club.id != clubId) throw PostNotFoundException()
        validateOwner(post, userId)
        validateWritePermission(post.board, member)

        markPostFilesDeleted(post.id)
        post.markDeleted()
    }

    private fun findBoardInClub(
        boardId: Long,
        clubId: Long,
    ): Board = boardRepository.findByIdAndClubIdAndIsDeletedFalse(boardId, clubId) ?: throw BoardNotFoundException()

    private fun findPost(postId: Long): Post =
        postRepository.findActivePostById(postId) ?: throw PostNotFoundException()

    private fun validateOwner(
        post: Post,
        userId: Long,
    ) {
        if (!post.isOwnedBy(userId)) {
            throw PostNotOwnedException()
        }
    }

    private fun validateWritePermission(
        board: Board,
        member: ClubMember,
    ) {
        if (!board.canWriteBy(member.memberRole)) {
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
