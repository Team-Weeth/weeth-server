package com.weeth.domain.notification.domain.vo

data class PushNotificationResult(
    val invalidTokens: List<String> = emptyList(),
    val outcomes: List<PushTargetOutcome> = emptyList(),
)
