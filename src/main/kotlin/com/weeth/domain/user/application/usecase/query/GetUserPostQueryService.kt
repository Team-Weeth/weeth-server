package com.weeth.domain.user.application.usecase.query

import com.weeth.domain.board.domain.repository.PostReader
import com.weeth.domain.club.application.exception.ClubMemberNotFoundException
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.user.application.dto.response.UserMyPostResponse
import com.weeth.domain.user.application.exception.UserPageNotFoundException
import com.weeth.domain.user.application.mapper.UserPostMapper
import com.weeth.global.common.response.SliceResponse
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class GetUserPostQueryService(
    private val postReader: PostReader,
    private val clubMemberPolicy: ClubMemberPolicy,
    private val userPostMapper: UserPostMapper,
    private val clock: Clock,
) {
    companion object {
        private const val MAX_PAGE_SIZE = 50
    }

    fun getMyPosts(
        userId: Long,
        clubId: Long,
        pageNumber: Int,
        pageSize: Int,
    ): SliceResponse<UserMyPostResponse> {
        validatePage(pageNumber, pageSize)
        clubMemberPolicy.getActiveMember(clubId, userId)
        val pageable = PageRequest.of(pageNumber, pageSize)
        val now = LocalDateTime.now(clock)
        val posts = postReader.findMyActivePosts(userId, clubId, pageable)
        return SliceResponse.from(posts.map { userPostMapper.toMyPostResponse(it, now) })
    }

    @Transactional(readOnly = true)
    fun getMemberPosts(
        requesterId: Long,
        clubId: Long,
        targetClubMemberId: Long,
        pageNumber: Int,
        pageSize: Int,
    ): SliceResponse<UserMyPostResponse> {
        validatePage(pageNumber, pageSize)
        clubMemberPolicy.getActiveMember(clubId, requesterId)
        val target = clubMemberPolicy.getMemberInClub(clubId, targetClubMemberId)
        if (!target.isActive()) throw ClubMemberNotFoundException()

        val pageable = PageRequest.of(pageNumber, pageSize)
        val now = LocalDateTime.now(clock)
        val posts = postReader.findActivePostsByClubMemberId(targetClubMemberId, pageable)
        return SliceResponse.from(posts.map { userPostMapper.toMyPostResponse(it, now) })
    }

    private fun validatePage(
        pageNumber: Int,
        pageSize: Int,
    ) {
        if (pageNumber < 0 || pageSize !in 1..MAX_PAGE_SIZE) {
            throw UserPageNotFoundException()
        }
    }
}
