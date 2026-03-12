package com.weeth.domain.user.application.usecase.query

import com.weeth.domain.user.application.dto.response.UserProfileResponse
import com.weeth.domain.user.application.dto.response.UserSummaryResponse
import com.weeth.domain.user.application.mapper.UserMapper
import com.weeth.domain.user.domain.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetUserQueryService(
    private val userRepository: UserRepository,
    private val mapper: UserMapper,
) {
    fun existsByEmail(email: String): Boolean = userRepository.existsByEmailValue(email)

    fun findMyProfile(userId: Long): UserProfileResponse {
        val user = userRepository.getById(userId)
        return mapper.toUserProfileResponse(user)
    }

    fun findMyInfo(userId: Long): UserSummaryResponse {
        val user = userRepository.getById(userId)
        return mapper.toUserSummaryResponse(user)
    }
}
