package com.weeth.domain.user.application.mapper

import com.weeth.domain.user.application.dto.response.SocialLoginResponse
import com.weeth.domain.user.application.dto.response.UserProfileResponse
import com.weeth.domain.user.application.dto.response.UserSummaryResponse
import com.weeth.domain.user.domain.entity.User
import com.weeth.global.auth.jwt.application.dto.JwtDto
import org.springframework.stereotype.Component

@Component
class UserMapper {
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
    ): UserProfileResponse =
        UserProfileResponse(
            user.id,
            user.name,
            user.emailValue,
            user.studentId,
            user.telValue,
            user.department,
            emptyList(),
            user.role,
        )

    fun toUserSummaryResponse(
        user: User,
    ): UserSummaryResponse =
        UserSummaryResponse(
            user.id,
            user.name,
            emptyList(),
            user.role,
        )
}
