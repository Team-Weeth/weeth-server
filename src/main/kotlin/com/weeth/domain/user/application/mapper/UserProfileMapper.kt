package com.weeth.domain.user.application.mapper

import com.weeth.domain.file.domain.port.FileAccessUrlPort
import com.weeth.domain.user.application.dto.request.CreateMultiProfileRequest
import com.weeth.domain.user.application.dto.response.UserProfileClubResponse
import com.weeth.domain.user.application.dto.response.UserProfileResponse
import com.weeth.domain.user.application.dto.response.UserProfilesResponse
import com.weeth.domain.user.domain.entity.User
import com.weeth.domain.user.domain.entity.UserProfile
import org.springframework.stereotype.Component

@Component
class UserProfileMapper(
    private val fileAccessUrlPort: FileAccessUrlPort,
) {
    fun toEntity(
        user: User,
        request: CreateMultiProfileRequest,
    ): UserProfile =
        UserProfile.create(
            user = user,
            name = request.name,
            profileImageStorageKey = request.profileImage?.storageKey,
            headerImageStorageKey = request.headerImage?.storageKey,
            bio = request.bio,
        )

    fun toResponse(
        profile: UserProfile,
        usingClubs: List<UserProfileClubResponse> = emptyList(),
    ): UserProfileResponse =
        UserProfileResponse(
            profileId = profile.id,
            name = profile.name,
            profileImageUrl = resolveImage(profile.profileImageStorageKey),
            headerImageUrl = resolveImage(profile.headerImageStorageKey),
            bio = profile.bio,
            usingClubs = usingClubs,
        )

    fun toListResponse(profiles: List<UserProfileResponse>): UserProfilesResponse = UserProfilesResponse(profiles)

    private fun resolveImage(storageKey: String?): String? = storageKey?.let(fileAccessUrlPort::resolve)
}
