package com.weeth.domain.user.application.mapper

import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.file.domain.port.FileAccessUrlPort
import com.weeth.domain.user.application.dto.response.UserMyPageInfoResponse
import com.weeth.domain.user.application.dto.response.UserMyPageResponse
import com.weeth.domain.user.application.dto.response.UserMyPageStatsResponse
import com.weeth.domain.user.application.dto.response.UserMyPageUsingProfileResponse
import com.weeth.domain.user.application.dto.response.UserProfileClubResponse
import com.weeth.domain.user.domain.entity.User
import com.weeth.domain.user.domain.entity.UserProfile
import com.weeth.global.common.id.TsidBase62Encoder
import org.springframework.stereotype.Component

@Component
class UserMyPageMapper(
    private val fileAccessUrlPort: FileAccessUrlPort,
) {
    fun toResponse(
        user: User,
        postCount: Long,
        attendedSessionCount: Long,
        usingProfileMembers: List<ClubMember>,
    ): UserMyPageResponse =
        UserMyPageResponse(
            user = toInfoResponse(user),
            stats = UserMyPageStatsResponse(postCount = postCount, attendedSessionCount = attendedSessionCount),
            usingProfiles = toUsingProfiles(usingProfileMembers),
        )

    private fun toInfoResponse(user: User): UserMyPageInfoResponse =
        UserMyPageInfoResponse(
            name = user.name,
            tel = user.telValue,
            email = user.emailValue,
            school = user.school,
            department = user.department,
            studentId = user.studentId,
        )

    private fun toUsingProfiles(members: List<ClubMember>): List<UserMyPageUsingProfileResponse> =
        members
            .mapNotNull { member -> member.userProfile?.let { profile -> profile to member } }
            .groupBy { it.first.id }
            .values
            .sortedBy { it.first().first.id }
            .map { group ->
                toUsingProfile(
                    profile = group.first().first,
                    members = group.map { it.second },
                )
            }

    private fun toUsingProfile(
        profile: UserProfile,
        members: List<ClubMember>,
    ): UserMyPageUsingProfileResponse =
        UserMyPageUsingProfileResponse(
            profileId = profile.id,
            name = profile.name,
            profileImageUrl = resolveImage(profile.profileImageStorageKey),
            headerImageUrl = resolveImage(profile.headerImageStorageKey),
            bio = profile.bio,
            clubs =
                members
                    .sortedBy { it.club.id }
                    .map {
                        UserProfileClubResponse(
                            clubId = TsidBase62Encoder.encode(it.club.id),
                            name = it.club.name,
                        )
                    },
        )

    private fun resolveImage(storageKey: String?): String? = storageKey?.let(fileAccessUrlPort::resolve)
}
