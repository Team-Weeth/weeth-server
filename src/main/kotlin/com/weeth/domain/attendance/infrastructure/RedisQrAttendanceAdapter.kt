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
        sessionId: Long,
        code: Int,
    ) {
        redisTemplate.opsForValue().set(key(sessionId), code.toString(), QrAttendancePort.TTL_SECONDS, TimeUnit.SECONDS)
    }

    override fun getCode(sessionId: Long): Int? = redisTemplate.opsForValue().get(key(sessionId))?.toIntOrNull()

    private fun key(sessionId: Long) = "$PREFIX$sessionId"

    companion object {
        private const val PREFIX = "qr:"
    }
}
