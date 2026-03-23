package com.weeth.domain.user.application.usecase.query

import com.weeth.domain.club.domain.service.ClubMemberPolicy
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
    private val clubMemberPolicy: ClubMemberPolicy,
    private val mapper: UserMapper,
) {
    fun existsByEmail(email: String): Boolean = userRepository.existsByEmailValue(email)

    fun findMyProfile(
        clubId: Long,
        userId: Long,
    ): UserProfileResponse {
        val user = userRepository.getById(userId)
        val member = clubMemberPolicy.getActiveMember(clubId, userId)
        return mapper.toUserProfileResponse(user, member)
    }

    fun findMyInfo(
        clubId: Long,
        userId: Long,
    ): UserSummaryResponse {
        val user = userRepository.getById(userId)
        val member = clubMemberPolicy.getActiveMember(clubId, userId)
        return mapper.toUserSummaryResponse(user, member)
    }

    @Deprecated("WTH-205에서 club-scoped API로 대체 예정")
    fun findMyInfo(userId: Long): UserSummaryResponse {
        val user = userRepository.getById(userId)
        return mapper.toUserSummaryResponse(user)
    }
}
