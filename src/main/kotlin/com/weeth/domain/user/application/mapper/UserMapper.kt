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
            id = user.id,
            name = user.name,
            email = user.emailValue,
            studentId = user.studentId,
            tel = user.telValue,
            department = user.department,
            cardinals = toCardinalNumbers(userCardinals),
            role = user.role,
            bio = user.bio,
            profileImageUrl = user.profileImageUrl,
        )

    fun toAdminUserResponse(
        user: User,
        userCardinals: List<UserCardinal>,
    ): AdminUserResponse =
        AdminUserResponse(
            id = user.id,
            name = user.name,
            email = user.emailValue,
            studentId = user.studentId,
            tel = user.telValue,
            department = user.department,
            cardinals = toCardinalNumbers(userCardinals),
            status = user.status,
            role = user.role,
            attendanceCount = user.attendanceCount,
            absenceCount = user.absenceCount,
            attendanceRate = user.attendanceRate,
            penaltyCount = user.penaltyCount,
            createdAt = user.createdAt,
            modifiedAt = user.modifiedAt,
        )

    fun toUserSummaryResponse(
        user: User,
        userCardinals: List<UserCardinal>,
    ): UserSummaryResponse =
        UserSummaryResponse(
            id = user.id,
            name = user.name,
            cardinals = toCardinalNumbers(userCardinals),
            role = user.role,
        )

    fun toUserDetailsResponse(
        user: User,
        userCardinals: List<UserCardinal>,
    ): UserDetailsResponse =
        UserDetailsResponse(
            id = user.id,
            name = user.name,
            email = user.emailValue,
            studentId = user.studentId,
            department = user.department,
            cardinals = toCardinalNumbers(userCardinals),
            role = user.role,
            bio = user.bio,
            profileImageUrl = user.profileImageUrl,
        )

    private fun toCardinalNumbers(userCardinals: List<UserCardinal>): List<Int> {
        if (userCardinals.isEmpty()) {
            return emptyList()
        }
        return userCardinals.map { it.cardinal.cardinalNumber }
    }
}
