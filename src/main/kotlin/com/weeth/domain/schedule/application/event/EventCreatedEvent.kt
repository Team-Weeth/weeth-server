package com.weeth.domain.schedule.application.event

import java.time.LocalDateTime

data class EventCreatedEvent(
    val clubId: Long,
    val eventId: Long,
    val cardinalNumber: Int,
    val title: String,
    val start: LocalDateTime,
    val authorUserId: Long,
)
