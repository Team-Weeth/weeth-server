package com.weeth.domain.user.application.usecase.query

import com.weeth.domain.user.application.dto.response.UserProfileResponse
import com.weeth.domain.user.application.dto.response.UserProfilesResponse
import com.weeth.domain.user.application.exception.UserProfileNotFoundException
import com.weeth.domain.user.application.mapper.UserProfileMapper
import com.weeth.domain.user.domain.repository.UserProfileRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetUserProfileQueryService(
    private val userProfileRepository: UserProfileRepository,
    private val userProfileMapper: UserProfileMapper,
) {
    @Transactional(readOnly = true)
    fun findAll(userId: Long): UserProfilesResponse {
        val profiles =
            userProfileRepository
                .findAllByUserIdOrderByIdAsc(userId)
                .map(userProfileMapper::toResponse)
        return userProfileMapper.toListResponse(profiles)
    }

    @Transactional(readOnly = true)
    fun find(
        userId: Long,
        profileId: Long,
    ): UserProfileResponse {
        val profile =
            userProfileRepository
                .findByIdAndUserId(profileId, userId)
                .orElseThrow { UserProfileNotFoundException() }
        return userProfileMapper.toResponse(profile)
    }
}
