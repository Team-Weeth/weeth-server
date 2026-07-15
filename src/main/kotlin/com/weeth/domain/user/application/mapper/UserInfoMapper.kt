package com.weeth.domain.user.application.mapper

import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.file.domain.port.FileAccessUrlPort
import com.weeth.domain.user.application.dto.response.UserInfo
import org.springframework.stereotype.Component

@Component
class UserInfoMapper(
    private val fileAccessUrlPort: FileAccessUrlPort,
) {
    fun toClubMemberAuthorInfo(member: ClubMember): UserInfo =
        UserInfo.ofClubMemberProfile(
            clubMember = member,
            profileName = member.userProfile?.name ?: member.user.name,
            resolvedProfileImageUrl = resolveProfileImage(member),
        )

    private fun resolveProfileImage(member: ClubMember): String? {
        val storageKey = member.userProfile?.profileImageStorageKey ?: member.profileImageStorageKey
        return storageKey?.let { fileAccessUrlPort.resolve(it) }
    }
}
