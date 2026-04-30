package com.weeth.domain.attendance.infrastructure

import com.weeth.domain.attendance.domain.port.QrAttendancePort
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.time.LocalDateTime
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

    override fun getExpiredAt(sessionId: Long): LocalDateTime? {
        val ttl = redisTemplate.getExpire(key(sessionId), TimeUnit.SECONDS)
        return if (ttl > 0) LocalDateTime.now().plusSeconds(ttl) else null
    }

    private fun key(sessionId: Long) = "${QrAttendancePort.KEY_PREFIX}$sessionId"
}
