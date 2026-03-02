package com.weeth.domain.attendance.infrastructure

import com.weeth.domain.attendance.domain.port.QrAttendancePort
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class RedisQrAttendanceAdapter(
    private val redisTemplate: RedisTemplate<String, String>,
) : QrAttendancePort {
    override fun store(
        code: Int,
        sessionId: Long,
    ) {
        redisTemplate.opsForValue().set(key(code), sessionId.toString(), TTL_SECONDS, TimeUnit.SECONDS)
    }

    override fun getSessionId(code: Int): Long? = redisTemplate.opsForValue().get(key(code))?.toLongOrNull()

    private fun key(code: Int) = "$PREFIX$code"

    companion object {
        private const val PREFIX = "qr:"
        private const val TTL_SECONDS = 600L
    }
}
