package com.weeth.domain.user.domain.port

import com.weeth.domain.user.domain.enums.SocialProvider
import com.weeth.domain.user.domain.vo.SocialAuthResult

interface SocialAuthPort {
    fun provider(): SocialProvider

    fun authenticate(authCode: String): SocialAuthResult

    fun authenticateWithIdToken(idToken: String): SocialAuthResult =
        throw UnsupportedOperationException("${provider()}은(는) ID token 직접 인증을 지원하지 않습니다")
}
