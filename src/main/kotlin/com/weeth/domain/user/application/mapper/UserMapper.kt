package com.weeth.domain.user.application.mapper

import com.weeth.domain.user.application.dto.response.AdminUserResponse
import com.weeth.domain.user.application.dto.response.SocialLoginResponse
import com.weeth.domain.user.application.dto.response.UserDetailsResponse
import com.weeth.domain.user.application.dto.response.UserProfileResponse
import com.weeth.domain.user.application.dto.response.UserSummaryResponse
import com.weeth.domain.user.domain.entity.User
import com.weeth.domain.user.domain.entity.UserCardinal
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
        userCardinals: List<UserCardinal>,
    ): UserProfileResponse =
        UserProfileResponse(
            user.id,
            user.name,
            user.emailValue,
            user.studentId,
            user.telValue,
            user.department,
            toCardinalNumbers(userCardinals),
            user.role,
        )

    fun toAdminUserResponse(
        user: User,
        userCardinals: List<UserCardinal>,
    ): AdminUserResponse =
        AdminUserResponse(
            user.id,
            user.name,
            user.emailValue,
            user.studentId,
            user.telValue,
            user.department,
            toCardinalNumbers(userCardinals),
            user.status,
            user.role,
            user.attendanceCount,
            user.absenceCount,
            user.attendanceRate,
            user.penaltyCount,
            user.createdAt,
            user.modifiedAt,
        )

    fun toUserSummaryResponse(
        user: User,
        userCardinals: List<UserCardinal>,
    ): UserSummaryResponse =
        UserSummaryResponse(
            user.id,
            user.name,
            toCardinalNumbers(userCardinals),
            user.role,
        )

    fun toUserDetailsResponse(
        user: User,
        userCardinals: List<UserCardinal>,
    ): UserDetailsResponse =
        UserDetailsResponse(
            user.id,
            user.name,
            user.emailValue,
            user.studentId,
            user.department,
            toCardinalNumbers(userCardinals),
            user.role,
        )

    private fun toCardinalNumbers(userCardinals: List<UserCardinal>): List<Int> {
        if (userCardinals.isEmpty()) {
            return emptyList()
        }
        return userCardinals.map { it.cardinal.cardinalNumber }
    }
}
