package com.weeth.domain.user.domain.port

import com.weeth.domain.user.domain.enums.SocialProvider
import com.weeth.domain.user.domain.vo.SocialAuthResult

interface SocialAuthPort {
    fun provider(): SocialProvider

    fun authenticate(authCode: String): SocialAuthResult
}
