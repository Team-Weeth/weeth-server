package com.weeth.domain.user.application.mapper

import com.weeth.domain.file.domain.port.FileAccessUrlPort
import com.weeth.domain.user.application.dto.response.SocialLoginResponse
import com.weeth.global.auth.jwt.application.dto.JwtDto
import org.springframework.stereotype.Component

@Component
class UserMapper(
    private val fileAccessUrlPort: FileAccessUrlPort,
) {
    fun toSocialLoginResponse(
        token: JwtDto,
        registered: Boolean,
    ): SocialLoginResponse =
        SocialLoginResponse(
            accessToken = token.accessToken,
            refreshToken = token.refreshToken,
            registered = registered,
        )
}
