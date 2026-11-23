package com.weeth.domain.notification.domain.vo

data class NotificationTemplate(
    val version: Int,
    val titleTemplate: String,
    val bodyTemplate: String,
    val requiredVariables: Set<String>,
)
