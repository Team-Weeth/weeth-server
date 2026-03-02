package com.weeth.domain.user.domain.vo

import com.weeth.domain.user.domain.enums.SocialProvider

data class SocialAuthResult(
    val provider: SocialProvider,
    val providerUserId: String,
    val email: String,
    val emailVerified: Boolean,
    val profileImageUrl: String?,
    val name: String?,
)
