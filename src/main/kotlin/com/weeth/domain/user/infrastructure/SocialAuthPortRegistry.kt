package com.weeth.domain.user.infrastructure

import com.weeth.domain.user.domain.enums.SocialProvider
import com.weeth.domain.user.domain.port.SocialAuthPort
import org.springframework.stereotype.Component

@Component
class SocialAuthPortRegistry(
    ports: List<SocialAuthPort>,
) {
    private val portsByProvider = ports.associateBy { it.provider() }

    fun get(provider: SocialProvider): SocialAuthPort =
        requireNotNull(portsByProvider[provider]) { "소셜 로그인 제공자를 찾을 수 없습니다: $provider" }
}
