package com.weeth.domain.minigame.infrastructure

import com.weeth.domain.minigame.domain.port.WaxRankingPort
import com.weeth.domain.minigame.domain.vo.WaxRankEntry
import com.weeth.domain.minigame.domain.vo.WaxRankingBucket
import com.weeth.domain.minigame.domain.vo.WaxRankingPeriod
import com.weeth.domain.minigame.domain.vo.WaxScore
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.stereotype.Component

/**
 * 기간별 sorted set(멤버 = 토큰 해시, 점수 = [WaxRankingBucket.scoreOf])과 닉네임 hash로 랭킹을 저장한다.
 * 키는 기간마다 새로 생기고 [WaxRankingPeriod.retention] 뒤 만료된다.
 */
@Component
class RedisWaxRankingAdapter(
    // 공용 RedisTemplate은 hash 직렬화기가 지정돼 있지 않아 닉네임 hash를 문자열로 읽으려고 String 전용 템플릿을 쓴다.
    private val redisTemplate: StringRedisTemplate,
) : WaxRankingPort {
    override fun record(score: WaxScore) {
        WaxRankingPeriod.entries.forEach { period ->
            val bucket = period.bucketOf(score.recordedAt)
            redisTemplate.execute(
                recordScript,
                listOf(scoresKey(bucket), namesKey(bucket)),
                score.tokenHash,
                bucket.scoreOf(score.brokenWaxes, score.recordedAt).toLong().toString(),
                score.name,
                period.retention.seconds.toString(),
            )
        }
    }

    override fun top(
        bucket: WaxRankingBucket,
        limit: Int,
    ): List<WaxRankEntry> {
        val rows =
            redisTemplate
                .opsForZSet()
                .reverseRangeWithScores(
                    scoresKey(bucket),
                    0,
                    limit - 1L,
                ).orEmpty()
                .toList()
        if (rows.isEmpty()) return emptyList()
        val names = redisTemplate.opsForHash<String, String>().multiGet(namesKey(bucket), rows.mapNotNull { it.value })
        return rows.mapIndexed { index, row ->
            WaxRankEntry(index + 1, names.getOrNull(index) ?: "", WaxRankingBucket.brokenWaxesOf(row.score ?: 0.0))
        }
    }

    override fun find(
        bucket: WaxRankingBucket,
        tokenHash: String,
    ): WaxRankEntry? {
        val zset = redisTemplate.opsForZSet()
        val rank = zset.reverseRank(scoresKey(bucket), tokenHash) ?: return null
        val score = zset.score(scoresKey(bucket), tokenHash) ?: return null
        val name = redisTemplate.opsForHash<String, String>().get(namesKey(bucket), tokenHash) ?: ""
        return WaxRankEntry(rank.toInt() + 1, name, WaxRankingBucket.brokenWaxesOf(score))
    }

    private fun scoresKey(bucket: WaxRankingBucket) = "$KEY_PREFIX${bucket.id}"

    private fun namesKey(bucket: WaxRankingBucket) = "$KEY_PREFIX${bucket.id}:names"

    /** 최고 기록만 남기기(비교 후 쓰기)와 만료 갱신을 한 번에 처리해 동시 기록 사이에 끼어드는 일이 없게 한다. */
    private val recordScript =
        DefaultRedisScript(
            """
            local current = redis.call('ZSCORE', KEYS[1], ARGV[1])
            if (not current) or tonumber(ARGV[2]) > tonumber(current) then
              redis.call('ZADD', KEYS[1], ARGV[2], ARGV[1])
            end
            redis.call('HSET', KEYS[2], ARGV[1], ARGV[3])
            redis.call('EXPIRE', KEYS[1], ARGV[4])
            redis.call('EXPIRE', KEYS[2], ARGV[4])
            return 1
            """.trimIndent(),
            Long::class.java,
        )

    companion object {
        private const val KEY_PREFIX = "wax:rank:"
    }
}
