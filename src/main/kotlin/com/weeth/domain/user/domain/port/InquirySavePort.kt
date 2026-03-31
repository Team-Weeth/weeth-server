package com.weeth.domain.user.domain.port

interface InquirySavePort {
    fun save(
        email: String,
        message: String,
    )
}
