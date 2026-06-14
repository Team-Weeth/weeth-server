package com.weeth.global.auth.jwt.domain.port

interface AccessTokenBlacklistStorePort {
    fun blacklist(userId: Long)

    fun isBlacklisted(userId: Long): Boolean
}
