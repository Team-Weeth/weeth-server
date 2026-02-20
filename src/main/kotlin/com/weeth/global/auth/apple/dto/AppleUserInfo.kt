package com.weeth.global.auth.apple.dto

data class AppleUserInfo(
    val appleId: String,
    val email: String?,
    val emailVerified: Boolean,
)
