package com.weeth.domain.board.application.usecase.query

import com.weeth.domain.board.application.dto.response.PostDetailResponse
import com.weeth.domain.board.application.dto.response.PostListResponse
import com.weeth.domain.board.application.exception.BoardNotFoundException
import com.weeth.domain.board.application.exception.NoSearchResultException
import com.weeth.domain.board.application.exception.PageNotFoundException
import com.weeth.domain.board.application.exception.PostNotFoundException
import com.weeth.domain.board.application.mapper.PostMapper
import com.weeth.domain.board.domain.repository.BoardRepository
import com.weeth.domain.board.domain.repository.PostRepository
import com.weeth.domain.comment.application.usecase.query.GetCommentQueryService
import com.weeth.domain.comment.domain.repository.CommentReader
import com.weeth.domain.file.application.mapper.FileMapper
import com.weeth.domain.file.domain.entity.FileOwnerType
import com.weeth.domain.file.domain.repository.FileReader
import com.weeth.domain.user.domain.entity.enums.Role
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Slice
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class GetPostQueryService(
    private val postRepository: PostRepository,
    private val boardRepository: BoardRepository,
    private val commentReader: CommentReader,
    private val getCommentQueryService: GetCommentQueryService,
    private val fileReader: FileReader,
    private val fileMapper: FileMapper,
    private val postMapper: PostMapper,
) {
    companion object {
        private const val MAX_PAGE_SIZE = 50
    }

    fun findPost(
        postId: Long,
        role: Role,
    ): PostDetailResponse {
        val post = postRepository.findByIdAndIsDeletedFalse(postId) ?: throw PostNotFoundException()
        if (!post.board.isAccessibleBy(role)) {
            throw PostNotFoundException()
        }

        val files = fileReader.findAll(FileOwnerType.POST, post.id).map(fileMapper::toFileResponse)
        val comments = commentReader.findAllByPostId(post.id)
        val commentTree = getCommentQueryService.toCommentTreeResponses(comments)

        return postMapper.toDetailResponse(post, commentTree, files)
    }

    fun findPosts(
        boardId: Long,
        pageNumber: Int,
        pageSize: Int,
        role: Role,
    ): Slice<PostListResponse> {
        validatePage(pageNumber, pageSize)
        validateBoardVisibility(boardId, role)
        val pageable = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "id"))
        val posts = postRepository.findAllActiveByBoardId(boardId, pageable)

        val postIds = posts.content.map { it.id }
        val fileExistsByPostId = buildFileExistsMap(postIds)
        val now = LocalDateTime.now()

        return posts.map { postMapper.toListResponse(it, fileExistsByPostId[it.id] == true, now) }
    }

    fun searchPosts(
        boardId: Long,
        keyword: String,
        pageNumber: Int,
        pageSize: Int,
        role: Role,
    ): Slice<PostListResponse> {
        validatePage(pageNumber, pageSize)
        validateBoardVisibility(boardId, role)
        val pageable = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "id"))
        val posts = postRepository.searchByBoardId(boardId, keyword.trim(), pageable)

        if (posts.isEmpty) {
            throw NoSearchResultException()
        }

        val postIds = posts.content.map { it.id }
        val fileExistsByPostId = buildFileExistsMap(postIds)
        val now = LocalDateTime.now()

        return posts.map { postMapper.toListResponse(it, fileExistsByPostId[it.id] == true, now) }
    }

    private fun validatePage(
        pageNumber: Int,
        pageSize: Int,
    ) {
        if (pageNumber < 0 || pageSize !in 1..MAX_PAGE_SIZE) {
            throw PageNotFoundException()
        }
    }

    private fun buildFileExistsMap(postIds: List<Long>): Map<Long, Boolean> {
        if (postIds.isEmpty()) {
            return emptyMap()
        }
        val filesGrouped = fileReader.findAll(FileOwnerType.POST, postIds).groupBy { it.ownerId }
        return postIds.associateWith { filesGrouped.containsKey(it) }
    }

    private fun validateBoardVisibility(
        boardId: Long,
        role: Role,
    ) {
        val board = boardRepository.findByIdAndIsDeletedFalse(boardId) ?: throw BoardNotFoundException()
        if (!board.isAccessibleBy(role)) {
            throw BoardNotFoundException()
        }
    }
}
