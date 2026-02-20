package com.weeth.global.auth.model

import com.weeth.domain.user.domain.entity.enums.Role

/**
 * Authentication 설정을 위한 model
 */
data class AuthenticatedUser(
    val id: Long,
    val email: String,
    val role: Role,
)
