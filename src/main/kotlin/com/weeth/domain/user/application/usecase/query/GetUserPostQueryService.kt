package com.weeth.domain.user.application.usecase.query

import com.weeth.domain.board.domain.repository.PostReader
import com.weeth.domain.user.application.dto.response.UserMyPostResponse
import com.weeth.domain.user.application.mapper.UserPostMapper
import com.weeth.global.common.response.PageResponse
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetUserPostQueryService(
    private val postReader: PostReader,
    private val userPostMapper: UserPostMapper,
) {
    @Transactional(readOnly = true)
    fun getMyPosts(
        userId: Long,
        pageNumber: Int,
        pageSize: Int,
    ): PageResponse<UserMyPostResponse> {
        val pageable = PageRequest.of(pageNumber, pageSize)
        val posts = postReader.findMyActivePosts(userId, pageable)
        return PageResponse.from(posts.map(userPostMapper::toMyPostResponse))
    }
}
