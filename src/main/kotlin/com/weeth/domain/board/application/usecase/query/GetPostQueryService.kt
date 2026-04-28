package com.weeth.domain.board.application.usecase.query

import com.weeth.domain.board.application.dto.response.PostDetailResponse
import com.weeth.domain.board.application.dto.response.PostListResponse
import com.weeth.domain.board.application.exception.BoardNotFoundException
import com.weeth.domain.board.application.exception.NoSearchResultException
import com.weeth.domain.board.application.exception.PageNotFoundException
import com.weeth.domain.board.application.exception.PostNotFoundException
import com.weeth.domain.board.application.mapper.PostMapper
import com.weeth.domain.board.domain.entity.Post
import com.weeth.domain.board.domain.repository.BoardRepository
import com.weeth.domain.board.domain.repository.PostLikeRepository
import com.weeth.domain.board.domain.repository.PostRepository
import com.weeth.domain.club.domain.enums.MemberRole
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.comment.application.usecase.query.GetCommentQueryService
import com.weeth.domain.comment.domain.repository.CommentReader
import com.weeth.domain.file.application.mapper.FileMapper
import com.weeth.domain.file.domain.entity.File
import com.weeth.domain.file.domain.enums.FileOwnerType
import com.weeth.domain.file.domain.repository.FileReader
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Slice
import org.springframework.data.domain.SliceImpl
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class GetPostQueryService(
    private val postRepository: PostRepository,
    private val boardRepository: BoardRepository,
    private val postLikeRepository: PostLikeRepository,
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
        boardId: Long,
        postId: Long,
    ): PostDetailResponse {
        val member = clubMemberPolicy.getActiveMember(clubId, userId)
        val post = postRepository.findByIdAndIsDeletedFalse(postId) ?: throw PostNotFoundException()

        if (post.board.id != boardId) throw BoardNotFoundException()
        if (post.board.club.id != clubId || post.board.isDeleted || !post.board.isAccessibleBy(member.memberRole)) {
            throw PostNotFoundException()
        }

        val files = fileReader.findAll(FileOwnerType.POST, post.id).map(fileMapper::toFileResponse)
        val comments = commentReader.findAllByPostId(post.id)
        val commentTree = getCommentQueryService.toCommentTreeResponses(comments)
        val isLiked = postLikeRepository.existsByPostAndUserIdAndIsActiveTrue(post, userId)
        val now = LocalDateTime.now()

        return postMapper.toDetailResponse(post, commentTree, files, isLiked, now, member.memberRole)
    }

    fun findAllPosts(
        clubId: Long,
        userId: Long,
        pageNumber: Int,
        pageSize: Int,
    ): Slice<PostListResponse> {
        val member = clubMemberPolicy.getActiveMember(clubId, userId)
        validatePage(pageNumber, pageSize)

        val accessibleBoardIds =
            boardRepository
                .findAllByClubIdAndIsDeletedFalseOrderByDisplayOrderAscIdAsc(clubId)
                .filter { it.isAccessibleBy(member.memberRole) }
                .map { it.id }

        val pageable = PageRequest.of(pageNumber, pageSize)

        if (accessibleBoardIds.isEmpty()) {
            return SliceImpl(emptyList(), pageable, false)
        }

        val posts = postRepository.findAllActiveByBoardIds(accessibleBoardIds, pageable)

        return toPostListResponses(posts, userId, member.memberRole)
    }

    fun findPosts(
        clubId: Long,
        userId: Long,
        boardId: Long,
        pageNumber: Int,
        pageSize: Int,
    ): Slice<PostListResponse> {
        val member = clubMemberPolicy.getActiveMember(clubId, userId)
        validatePage(pageNumber, pageSize)
        validateBoardVisibility(boardId, clubId, member.memberRole)

        val pageable = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "id"))
        val posts = postRepository.findAllActiveByBoardId(boardId, pageable)

        return toPostListResponses(posts, userId, member.memberRole)
    }

    fun searchPosts(
        clubId: Long,
        userId: Long,
        boardId: Long,
        keyword: String,
        pageNumber: Int,
        pageSize: Int,
    ): Slice<PostListResponse> {
        val member = clubMemberPolicy.getActiveMember(clubId, userId)
        validatePage(pageNumber, pageSize)
        validateBoardVisibility(boardId, clubId, member.memberRole)
        val pageable = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "id"))
        val posts = postRepository.searchByBoardId(boardId, keyword.trim(), pageable)

        if (posts.isEmpty) {
            throw NoSearchResultException()
        }

        return toPostListResponses(posts, userId, member.memberRole)
    }

    private fun toPostListResponses(
        posts: Slice<Post>,
        userId: Long,
        memberRole: MemberRole,
    ): Slice<PostListResponse> {
        val postIds = posts.content.map { it.id }
        val filesByPostId = buildFileMap(postIds)
        val likedPostIds = postLikeRepository.findLikedPostIds(postIds, userId)
        val now = LocalDateTime.now()

        return posts.map { post ->
            postMapper.toListResponse(
                post,
                filesByPostId[post.id]?.map(fileMapper::toFileResponse) ?: emptyList(),
                now,
                post.id in likedPostIds,
                memberRole,
            )
        }
    }

    private fun validatePage(
        pageNumber: Int,
        pageSize: Int,
    ) {
        if (pageNumber < 0 || pageSize !in 1..MAX_PAGE_SIZE) {
            throw PageNotFoundException()
        }
    }

    private fun buildFileMap(postIds: List<Long>): Map<Long, List<File>> {
        if (postIds.isEmpty()) return emptyMap()
        return fileReader.findAll(FileOwnerType.POST, postIds).groupBy { it.ownerId }
    }

    private fun validateBoardVisibility( // todo: 볼 권한이 없는 경우 권한 관련 예외를 던져주는게 나을지 UX 상의 후 결정
        boardId: Long,
        clubId: Long,
        memberRole: MemberRole,
    ) {
        val board =
            boardRepository.findByIdAndClubIdAndIsDeletedFalse(boardId, clubId) ?: throw BoardNotFoundException()
        if (!board.isAccessibleBy(memberRole)) {
            throw BoardNotFoundException()
        }
    }
}
