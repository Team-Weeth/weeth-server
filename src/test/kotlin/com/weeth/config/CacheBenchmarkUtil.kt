package com.weeth.config

import kotlin.system.measureTimeMillis

/**
 * Redis Cache 캐시 미스/히트 성능 측정 유틸.
 *
 * - [benchmarkRounds]: 1번 실제 호출(캐시 미스) + N-1번 캐시 히트로 성능 차이를 측정한다.
 *   캐싱 없이 N번 호출했을 경우의 총 시간은 실측값이 아닌 추정치(missTimeMs × N)이다.
 *
 * 사용법:
 * ```
 * val result = CacheBenchmarkUtil.benchmarkRounds(
 *     cacheName = "schools",
 *     rounds = 20,
 *     clearCache = { cacheManager.getCache("schools")?.clear() },
 * ) { getUniversityQueryService.getSchools() }
 * println(result)
 * result.totalWithCacheMs shouldBeLessThan result.estimatedTotalWithoutCacheMs
 * ```
 */
object CacheBenchmarkUtil {
    data class MultiRoundResult(
        val cacheName: String,
        val rounds: Int,
        val missTimeMs: Long,
        val avgHitTimeMs: Long,
    ) {
        // 실측값이 아닌 추정치: 캐싱 없이 N번 호출한다고 가정한 예상 시간
        val estimatedTotalWithoutCacheMs: Long get() = missTimeMs * rounds
        val totalWithCacheMs: Long get() = missTimeMs + avgHitTimeMs * (rounds - 1)
        val hitRate: Double get() = (rounds - 1).toDouble() / rounds * 100
        val speedup: Long get() = estimatedTotalWithoutCacheMs / totalWithCacheMs.coerceAtLeast(1)

        override fun toString(): String {
            val maxMs = estimatedTotalWithoutCacheMs.coerceAtLeast(1)
            val noBar = "█".repeat((estimatedTotalWithoutCacheMs * BAR_WIDTH / maxMs).toInt().coerceAtLeast(1))
            val withBar = "█".repeat((totalWithCacheMs * BAR_WIDTH / maxMs).toInt().coerceAtLeast(1))
            return """
                |[CacheBenchmark][$cacheName] $rounds rounds
                |  without cache │$noBar ~${estimatedTotalWithoutCacheMs}ms (${missTimeMs}ms × $rounds 추정)
                |  with cache    │$withBar ${totalWithCacheMs}ms (${missTimeMs}ms + ${rounds - 1} × ${avgHitTimeMs}ms)
                |  hit rate: ${"%.1f".format(hitRate)}% (${rounds - 1}/$rounds)
                |  speedup: ${speedup}x
                """.trimMargin()
        }
    }

    private const val BAR_WIDTH = 40

    fun benchmarkRounds(
        cacheName: String,
        rounds: Int,
        clearCache: () -> Unit,
        block: () -> Unit,
    ): MultiRoundResult {
        clearCache()
        val missTimeMs = measureTimeMillis { block() }

        val hitTimes = (1 until rounds).map { measureTimeMillis { block() } }
        val avgHitTimeMs = if (hitTimes.isEmpty()) 0L else hitTimes.average().toLong()

        return MultiRoundResult(
            cacheName = cacheName,
            rounds = rounds,
            missTimeMs = missTimeMs,
            avgHitTimeMs = avgHitTimeMs,
        )
    }
}
