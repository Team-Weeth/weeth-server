package com.weeth.domain.user.application.usecase.query

import com.weeth.domain.club.domain.enums.MemberStatus
import com.weeth.domain.club.domain.repository.ClubMemberReader
import com.weeth.domain.user.application.dto.response.UserProfileClubResponse
import com.weeth.domain.user.application.dto.response.UserProfileResponse
import com.weeth.domain.user.application.dto.response.UserProfilesResponse
import com.weeth.domain.user.application.exception.UserProfileNotFoundException
import com.weeth.domain.user.application.mapper.UserProfileMapper
import com.weeth.domain.user.domain.repository.UserProfileRepository
import com.weeth.global.common.id.TsidBase62Encoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetUserProfileQueryService(
    private val userProfileRepository: UserProfileRepository,
    private val clubMemberReader: ClubMemberReader,
    private val userProfileMapper: UserProfileMapper,
) {
    @Transactional(readOnly = true)
    fun findAll(userId: Long): UserProfilesResponse {
        val usingClubsByProfileId = findUsingClubsByProfileId(userId)
        val profiles =
            userProfileRepository
                .findAllByUserIdOrderByIdAsc(userId)
                .map { profile ->
                    userProfileMapper.toResponse(
                        profile = profile,
                        usingClubs = usingClubsByProfileId[profile.id].orEmpty(),
                    )
                }
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
        val usingClubsByProfileId = findUsingClubsByProfileId(userId)
        return userProfileMapper.toResponse(
            profile = profile,
            usingClubs = usingClubsByProfileId[profile.id].orEmpty(),
        )
    }

    private fun findUsingClubsByProfileId(userId: Long): Map<Long, List<UserProfileClubResponse>> =
        clubMemberReader
            .findAllByUserIdAndMemberStatusWithClubAndUserProfile(userId, MemberStatus.ACTIVE)
            .mapNotNull { member ->
                member.userProfile?.let { profile ->
                    profile.id to
                        UserProfileClubResponse(
                            clubId = TsidBase62Encoder.encode(member.club.id),
                            name = member.club.name,
                        )
                }
            }.groupBy(
                keySelector = { it.first },
                valueTransform = { it.second },
            )
}
