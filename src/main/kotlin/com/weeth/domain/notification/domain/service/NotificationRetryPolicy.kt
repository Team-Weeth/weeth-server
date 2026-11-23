package com.weeth.domain.notification.domain.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.concurrent.ThreadLocalRandom

@Service
class NotificationRetryPolicy(
    @Value("\${notification.dispatch.max-attempts:5}") val maxAttempts: Int = 5,
    @Value("\${notification.dispatch.retry-delays-seconds:10,30,120,600}") private val delays: List<Long> =
        listOf(10, 30, 120, 600),
) {
    init {
        require(maxAttempts > 0)
        require(delays.isNotEmpty() && delays.all { it in 1..36000 })
    }

    fun delaySeconds(attempt: Int): Long {
        val base = delays[(attempt - 1).coerceIn(0, delays.lastIndex)]
        return base + ThreadLocalRandom.current().nextLong(base / 5 + 1)
    }

    fun outboxDelaySeconds(attempt: Int): Long = (1L shl attempt.coerceIn(1, 8)).coerceAtMost(300)
}
