package com.weeth.domain.user.infrastructure

import com.weeth.domain.user.domain.enums.SocialProvider
import com.weeth.domain.user.domain.port.SocialAuthPort
import com.weeth.domain.user.domain.vo.SocialAuthResult
import com.weeth.global.auth.apple.AppleAuthService
import com.weeth.global.auth.apple.dto.AppleUserInfo
import org.springframework.stereotype.Component

@Component
class AppleSocialAuthAdapter(
    private val appleAuthService: AppleAuthService,
) : SocialAuthPort {
    override fun provider(): SocialProvider = SocialProvider.APPLE

    override fun authenticate(authCode: String): SocialAuthResult {
        val appleToken = appleAuthService.getAppleToken(authCode)
        val userInfo = appleAuthService.verifyAndDecodeIdToken(appleToken.idToken)
        return toSocialAuthResult(userInfo)
    }

    override fun authenticateWithIdToken(idToken: String): SocialAuthResult {
        val userInfo = appleAuthService.verifyAndDecodeIdToken(idToken)
        return toSocialAuthResult(userInfo)
    }

    private fun toSocialAuthResult(userInfo: AppleUserInfo): SocialAuthResult =
        SocialAuthResult(
            provider = SocialProvider.APPLE,
            providerUserId = userInfo.appleId,
            email = userInfo.email?.trim()?.lowercase() ?: "",
            emailVerified = userInfo.emailVerified,
            name = userInfo.name?.trim()?.takeIf { it.isNotBlank() },
        )
}
