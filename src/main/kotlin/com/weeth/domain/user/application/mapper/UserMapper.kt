package com.weeth.domain.user.application.mapper

import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.file.domain.port.FileAccessUrlPort
import com.weeth.domain.user.application.dto.response.SocialLoginResponse
import com.weeth.domain.user.application.dto.response.UserProfileResponse
import com.weeth.domain.user.application.dto.response.UserSummaryResponse
import com.weeth.domain.user.domain.entity.User
import com.weeth.global.auth.jwt.application.dto.JwtDto
import org.springframework.stereotype.Component

@Component
class UserMapper(
    private val fileAccessUrlPort: FileAccessUrlPort,
) {
    fun toSocialLoginResponse(
        token: JwtDto,
        isNewUser: Boolean,
    ): SocialLoginResponse =
        SocialLoginResponse(
            accessToken = token.accessToken,
            refreshToken = token.refreshToken,
            isNewUser = isNewUser,
        )

    fun toUserProfileResponse(
        user: User,
        clubMember: ClubMember,
    ): UserProfileResponse =
        UserProfileResponse(
            id = user.id,
            name = user.name,
            email = user.emailValue,
            studentId = user.studentId,
            tel = user.telValue,
            school = user.school,
            department = user.department,
            cardinals = emptyList(),
            role = clubMember.memberRole,
            profileImageUrl = clubMember.profileImageStorageKey?.let { fileAccessUrlPort.resolve(it) },
        )

    fun toUserSummaryResponse(
        user: User,
        clubMember: ClubMember,
    ): UserSummaryResponse =
        UserSummaryResponse(
            id = user.id,
            name = user.name,
            cardinals = emptyList(),
            role = clubMember.memberRole,
        )

    fun toUserSummaryResponse(user: User): UserSummaryResponse =
        UserSummaryResponse(
            id = user.id,
            name = user.name,
            cardinals = emptyList(),
            role = null,
        )
}
