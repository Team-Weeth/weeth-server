package com.weeth.domain.board.domain.vo

import com.weeth.domain.club.domain.enums.MemberRole

data class BoardConfig(
    val commentEnabled: Boolean = true,
    val writePermission: MemberRole = MemberRole.USER,
    val isPrivate: Boolean = false,
)
