package com.weeth.domain.notification.domain.vo

import com.weeth.domain.notification.domain.enums.PushTargetStatus

data class PushTargetOutcome(
    val token: String,
    val status: PushTargetStatus,
    val errorCode: String? = null,
)
