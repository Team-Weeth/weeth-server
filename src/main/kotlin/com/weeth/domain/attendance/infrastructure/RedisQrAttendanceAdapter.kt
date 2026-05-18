package com.weeth.domain.attendance.infrastructure

import com.weeth.domain.attendance.domain.port.QrAttendancePort
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
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
        redisTemplate.execute(
            storeScript,
            listOf(key(sessionId), activeKey(clubId)),
            code.toString(),
            sessionId.toString(),
            QrAttendancePort.TTL_SECONDS.toString(),
            QrAttendancePort.ACTIVE_TTL_SECONDS.toString(),
        )
    }

    override fun getCode(sessionId: Long): Int? = redisTemplate.opsForValue().get(key(sessionId))?.toIntOrNull()

    override fun getActiveSessionId(clubId: Long): Long? =
        redisTemplate.opsForValue().get(activeKey(clubId))?.toLongOrNull()

    override fun clearActiveSessionIfMatches(
        clubId: Long,
        sessionId: Long,
    ): Boolean =
        redisTemplate.execute(
            clearIfMatchesScript,
            listOf(activeKey(clubId)),
            sessionId.toString(),
        ) == 1L

    override fun getExpiredAt(sessionId: Long): LocalDateTime? {
        val ttl = redisTemplate.getExpire(key(sessionId), TimeUnit.SECONDS)
        return if (ttl > 0) LocalDateTime.now().plusSeconds(ttl) else null
    }

    private val storeScript =
        DefaultRedisScript(
            """
            redis.call('SET', KEYS[1], ARGV[1], 'EX', ARGV[3])
            redis.call('SET', KEYS[2], ARGV[2], 'EX', ARGV[4])
            return 1
            """.trimIndent(),
            Long::class.java,
        )

    private val clearIfMatchesScript =
        DefaultRedisScript(
            """
            if redis.call('GET', KEYS[1]) == ARGV[1] then
                return redis.call('DEL', KEYS[1])
            end
            return 0
            """.trimIndent(),
            Long::class.java,
        )

    private fun key(sessionId: Long) = "${QrAttendancePort.KEY_PREFIX}$sessionId"

    private fun activeKey(clubId: Long) = "${QrAttendancePort.ACTIVE_KEY_PREFIX}$clubId"
}
