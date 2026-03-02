package com.weeth.domain.board.domain.vo

import com.weeth.domain.user.domain.enums.Role

data class BoardConfig(
    val commentEnabled: Boolean = true,
    val writePermission: Role = Role.USER,
    val isPrivate: Boolean = false,
)
