package com.weeth.domain.board.domain.vo

data class BoardConfig(
    val commentEnabled: Boolean = true,
    val writePermission: WritePermission = WritePermission.USER,
    val isPrivate: Boolean = false,
) {
    enum class WritePermission {
        ADMIN,
        USER,
    }

}
