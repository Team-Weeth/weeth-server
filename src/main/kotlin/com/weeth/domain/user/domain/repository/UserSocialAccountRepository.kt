package com.weeth.domain.user.domain.repository

import com.weeth.domain.user.domain.entity.UserSocialAccount
import com.weeth.domain.user.domain.entity.enums.SocialProvider
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface UserSocialAccountRepository : JpaRepository<UserSocialAccount, Long> {
    fun findByProviderAndProviderUserId(
        provider: SocialProvider,
        providerUserId: String,
    ): Optional<UserSocialAccount>
}
