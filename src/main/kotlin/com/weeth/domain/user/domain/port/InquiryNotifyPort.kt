package com.weeth.domain.user.domain.port

interface InquiryNotifyPort {
    fun notify(
        email: String,
        message: String?,
    )
}
