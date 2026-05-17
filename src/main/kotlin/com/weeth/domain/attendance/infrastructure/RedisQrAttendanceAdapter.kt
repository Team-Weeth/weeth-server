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
        clubId: Long,
        sessionId: Long,
        code: Int,
    ) {
        redisTemplate.opsForValue().set(key(sessionId), code.toString(), QrAttendancePort.TTL_SECONDS, TimeUnit.SECONDS)
        redisTemplate
            .opsForValue()
            .set(activeKey(clubId), sessionId.toString(), QrAttendancePort.ACTIVE_TTL_SECONDS, TimeUnit.SECONDS)
    }

    override fun getCode(sessionId: Long): Int? = redisTemplate.opsForValue().get(key(sessionId))?.toIntOrNull()

    override fun getActiveSessionId(clubId: Long): Long? =
        redisTemplate.opsForValue().get(activeKey(clubId))?.toLongOrNull()

    override fun clearActiveSessionIfMatches(
        clubId: Long,
        sessionId: Long,
    ): Boolean {
        val activeKey = activeKey(clubId)
        val activeSessionId = redisTemplate.opsForValue().get(activeKey)?.toLongOrNull() ?: return false
        if (activeSessionId != sessionId) return false

        redisTemplate.delete(activeKey)
        return true
    }

    override fun getExpiredAt(sessionId: Long): LocalDateTime? {
        val ttl = redisTemplate.getExpire(key(sessionId), TimeUnit.SECONDS)
        return if (ttl > 0) LocalDateTime.now().plusSeconds(ttl) else null
    }

    private fun key(sessionId: Long) = "${QrAttendancePort.KEY_PREFIX}$sessionId"

    private fun activeKey(clubId: Long) = "${QrAttendancePort.ACTIVE_KEY_PREFIX}$clubId"
}
