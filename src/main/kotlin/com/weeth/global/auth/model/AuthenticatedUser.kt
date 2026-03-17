package com.weeth.global.auth.model

/**
 * Authentication 설정을 위한 model
 */
data class AuthenticatedUser(
    val id: Long,
    val email: String,
)
