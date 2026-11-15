package com.weeth.domain.session.application.event

import java.time.LocalDateTime

data class SessionCreatedEvent(
    val clubId: Long,
    val sessionId: Long,
    val sessionGroupId: Long?,
    val cardinalNumber: Int,
    val title: String,
    val start: LocalDateTime,
    val sessionCount: Int,
    val authorUserId: Long,
)
