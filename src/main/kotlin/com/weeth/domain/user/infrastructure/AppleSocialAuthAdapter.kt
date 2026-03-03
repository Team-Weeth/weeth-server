package com.weeth.domain.user.infrastructure

import com.weeth.domain.user.domain.enums.SocialProvider
import com.weeth.domain.user.domain.port.SocialAuthPort
import com.weeth.domain.user.domain.vo.SocialAuthResult
import com.weeth.global.auth.apple.AppleAuthService
import org.springframework.stereotype.Component

@Component
class AppleSocialAuthAdapter(
    private val appleAuthService: AppleAuthService,
) : SocialAuthPort {
    override fun provider(): SocialProvider = SocialProvider.APPLE

    override fun authenticate(authCode: String): SocialAuthResult {
        val appleToken = appleAuthService.getAppleToken(authCode)
        val userInfo = appleAuthService.verifyAndDecodeIdToken(appleToken.idToken)
        val email = userInfo.email?.trim()?.lowercase() ?: ""
        val providerName = userInfo.name?.trim()?.takeIf { it.isNotBlank() }

        return SocialAuthResult(
            provider = SocialProvider.APPLE,
            providerUserId = userInfo.appleId,
            email = email,
            emailVerified = userInfo.emailVerified,
            profileImageUrl = null,
            name = providerName,
        )
    }
}
