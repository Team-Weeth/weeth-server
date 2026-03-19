package com.weeth.domain.user.infrastructure

import com.weeth.domain.user.application.exception.EmailNotFoundException
import com.weeth.domain.user.domain.enums.SocialProvider
import com.weeth.domain.user.domain.port.SocialAuthPort
import com.weeth.domain.user.domain.vo.SocialAuthResult
import com.weeth.global.auth.kakao.KakaoAuthService
import org.springframework.stereotype.Component

@Component
class KakaoSocialAuthAdapter(
    private val kakaoAuthService: KakaoAuthService,
) : SocialAuthPort {
    override fun provider(): SocialProvider = SocialProvider.KAKAO

    override fun authenticate(authCode: String): SocialAuthResult {
        val kakaoToken = kakaoAuthService.getKakaoToken(authCode)
        val userInfo = kakaoAuthService.getUserInfo(kakaoToken.accessToken)
        val account = userInfo.kakaoAccount
        val email = account.email?.trim()?.lowercase()
        val providerName =
            account.profile
                ?.nickname
                ?.trim()
                ?.takeIf { it.isNotBlank() }

        if (!account.isEmailValid || !account.isEmailVerified || email.isNullOrBlank()) {
            throw EmailNotFoundException()
        }

        return SocialAuthResult(
            provider = SocialProvider.KAKAO,
            providerUserId = userInfo.id.toString(),
            email = email,
            emailVerified = account.isEmailVerified,
            name = providerName,
        )
    }
}
