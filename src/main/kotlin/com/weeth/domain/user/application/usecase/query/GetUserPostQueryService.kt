package com.weeth.domain.user.application.usecase.query

import com.weeth.domain.board.domain.repository.PostReader
import com.weeth.domain.user.application.dto.response.UserMyPostResponse
import com.weeth.domain.user.application.exception.UserPageNotFoundException
import com.weeth.domain.user.application.mapper.UserPostMapper
import com.weeth.global.common.response.SliceResponse
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetUserPostQueryService(
    private val postReader: PostReader,
    private val userPostMapper: UserPostMapper,
) {
    companion object {
        private const val MAX_PAGE_SIZE = 50
    }

    @Transactional(readOnly = true)
    fun getMyPosts(
        userId: Long,
        pageNumber: Int,
        pageSize: Int,
    ): SliceResponse<UserMyPostResponse> {
        validatePage(pageNumber, pageSize)
        val pageable = PageRequest.of(pageNumber, pageSize)
        val posts = postReader.findMyActivePosts(userId, pageable)
        return SliceResponse.from(posts.map(userPostMapper::toMyPostResponse))
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
