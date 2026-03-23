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
import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.club.domain.enums.MemberRole
import com.weeth.domain.club.domain.repository.ClubMemberReader
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.comment.application.usecase.query.GetCommentQueryService
import com.weeth.domain.comment.domain.repository.CommentReader
import com.weeth.domain.file.application.mapper.FileMapper
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
    private val clubMemberPolicy: ClubMemberPolicy,
    private val clubMemberReader: ClubMemberReader,
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
    ): PostDetailResponse {
        val member = clubMemberPolicy.getActiveMember(clubId, userId)
        val post = postRepository.findByIdAndIsDeletedFalse(postId) ?: throw PostNotFoundException()

        if (post.board.club.id != clubId || post.board.isDeleted || !post.board.isAccessibleBy(member.memberRole)) {
            throw PostNotFoundException()
        }

        val files = fileReader.findAll(FileOwnerType.POST, post.id).map(fileMapper::toFileResponse)
        val comments = commentReader.findAllByPostId(post.id)

        val commentAuthorIds = comments.map { it.user.id }.distinct()
        val allAuthorIds = (commentAuthorIds + post.user.id).distinct()
        val memberMap = buildMemberMap(clubId, allAuthorIds)

        val commentTree = getCommentQueryService.toCommentTreeResponses(comments, memberMap)

        return postMapper.toDetailResponse(post, memberMap.getValue(post.user.id), commentTree, files)
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
            boardRepository.findAllByClubIdAndIsDeletedFalseOrderByIdAsc(clubId)
                .filter { it.isAccessibleBy(member.memberRole) }
                .map { it.id }

        val pageable = PageRequest.of(pageNumber, pageSize)

        if (accessibleBoardIds.isEmpty()) {
            return SliceImpl(emptyList(), pageable, false)
        }

        val posts = postRepository.findAllActiveByBoardIds(accessibleBoardIds, pageable)
        val memberMap = buildMemberMap(clubId, posts.content.map { it.user.id }.distinct())
        val fileExistsByPostId = buildFileExistsMap(posts.content.map { it.id })
        val now = LocalDateTime.now()

        return posts.map { post ->
            postMapper.toListResponse(post, memberMap.getValue(post.user.id), fileExistsByPostId[post.id] == true, now)
        }
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

        val postIds = posts.content.map { it.id }
        val fileExistsByPostId = buildFileExistsMap(postIds)
        val memberMap = buildMemberMap(clubId, posts.content.map { it.user.id }.distinct())
        val now = LocalDateTime.now()

        return posts.map { post ->
            postMapper.toListResponse(post, memberMap.getValue(post.user.id), fileExistsByPostId[post.id] == true, now)
        }
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

        val postIds = posts.content.map { it.id }
        val fileExistsByPostId = buildFileExistsMap(postIds)
        val memberMap = buildMemberMap(clubId, posts.content.map { it.user.id }.distinct())
        val now = LocalDateTime.now()

        return posts.map { post ->
            postMapper.toListResponse(post, memberMap.getValue(post.user.id), fileExistsByPostId[post.id] == true, now)
        }
    }

    /**
     * Post, Comment 조회 시 작성자 정보를 매핑하기 위한 헬퍼 메서드
     */
    private fun buildMemberMap(
        clubId: Long,
        userIds: List<Long>,
    ): Map<Long, ClubMember> {
        if (userIds.isEmpty()) return emptyMap()
        return clubMemberReader.findAllByClubIdAndUserIds(clubId, userIds).associateBy { it.user.id }
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
        memberRole: MemberRole,
    ) {
        val board =
            boardRepository.findByIdAndClubIdAndIsDeletedFalse(boardId, clubId) ?: throw BoardNotFoundException()
        if (!board.isAccessibleBy(memberRole)) {
            throw BoardNotFoundException()
        }
    }
}
