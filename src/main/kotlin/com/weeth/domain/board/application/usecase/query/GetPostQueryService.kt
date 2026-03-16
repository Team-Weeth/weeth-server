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
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.comment.application.usecase.query.GetCommentQueryService
import com.weeth.domain.comment.domain.repository.CommentReader
import com.weeth.domain.file.application.mapper.FileMapper
import com.weeth.domain.file.domain.enums.FileOwnerType
import com.weeth.domain.file.domain.repository.FileReader
import com.weeth.domain.user.domain.enums.Role
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
    private val clubMemberPolicy: ClubMemberPolicy,
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
        clubId: Long,
        userId: Long,
        postId: Long,
        role: Role, // TODO: 멀티 테넨시 지원으로 Jwt에 포함한 Role은 삭제 예정
    ): PostDetailResponse {
        clubMemberPolicy.getActiveMember(clubId, userId)
        val post = postRepository.findByIdAndIsDeletedFalse(postId) ?: throw PostNotFoundException()

        if (post.board.club.id != clubId || post.board.isDeleted || !post.board.isAccessibleBy(role)) {
            throw PostNotFoundException()
        }

        val files = fileReader.findAll(FileOwnerType.POST, post.id).map(fileMapper::toFileResponse)
        val comments = commentReader.findAllByPostId(post.id)
        val commentTree = getCommentQueryService.toCommentTreeResponses(comments)

        return postMapper.toDetailResponse(post, commentTree, files)
    }

    fun findPosts(
        clubId: Long,
        userId: Long,
        boardId: Long,
        pageNumber: Int,
        pageSize: Int,
        role: Role, // TODO: 멀티 테넨시 지원으로 Jwt에 포함한 Role은 삭제 예정
    ): Slice<PostListResponse> {
        clubMemberPolicy.getActiveMember(clubId, userId)
        validatePage(pageNumber, pageSize)
        validateBoardVisibility(boardId, clubId, role)

        val pageable = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "id"))
        val posts = postRepository.findAllActiveByBoardId(boardId, pageable)

        val postIds = posts.content.map { it.id }
        val fileExistsByPostId = buildFileExistsMap(postIds)
        val now = LocalDateTime.now()

        return posts.map { postMapper.toListResponse(it, fileExistsByPostId[it.id] == true, now) }
    }

    fun searchPosts(
        clubId: Long,
        userId: Long,
        boardId: Long,
        keyword: String,
        pageNumber: Int,
        pageSize: Int,
        role: Role, // TODO: 멀티 테넨시 지원으로 Jwt에 포함한 Role은 삭제 예정
    ): Slice<PostListResponse> {
        clubMemberPolicy.getActiveMember(clubId, userId)
        validatePage(pageNumber, pageSize)
        validateBoardVisibility(boardId, clubId, role)
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

    private fun validateBoardVisibility( // todo: 볼 권한이 없는 경우 권한 관련 예외를 던져주는게 나을지 UX 상의 후 결정
        boardId: Long,
        clubId: Long,
        role: Role,
    ) {
        val board =
            boardRepository.findByIdAndClubIdAndIsDeletedFalse(boardId, clubId) ?: throw BoardNotFoundException()
        if (!board.isAccessibleBy(role)) {
            throw BoardNotFoundException()
        }
    }
}
