package com.weeth.config

import jakarta.persistence.EntityManager
import org.hibernate.SessionFactory

/**
 * Hibernate Statistics 기반 쿼리 카운터.
 * 블록 실행 중 발생한 SQL prepared statement 수를 반환한다.
 *
 * 사용법:
 * ```
 * val result = QueryCountUtil.count(entityManager) {
 *     repository.findById(id)
 * }
 * result.queryCount shouldBe 1
 * ```
 */
object QueryCountUtil {
    data class Result(
        val queryCount: Long,
        val entityLoadCount: Long,
        val collectionLoadCount: Long,
        val elapsedTimeMs: Double,
    ) {
        override fun toString(): String =
            "queries=$queryCount, entityLoads=$entityLoadCount, collectionLoads=$collectionLoadCount, elapsedMs=%.3f".format(
                elapsedTimeMs,
            )
    }

    fun count(
        entityManager: EntityManager,
        block: () -> Unit,
    ): Result {
        val sessionFactory = entityManager.entityManagerFactory.unwrap(SessionFactory::class.java)
        val stats = sessionFactory.statistics

        stats.isStatisticsEnabled = true
        stats.clear()

        val startNanos = System.nanoTime()
        block()
        val elapsedNanos = System.nanoTime() - startNanos

        val result =
            Result(
                queryCount = stats.prepareStatementCount,
                entityLoadCount = stats.entityLoadCount,
                collectionLoadCount = stats.collectionFetchCount,
                elapsedTimeMs = elapsedNanos / 1_000_000.0,
            )

        stats.isStatisticsEnabled = false
        return result
    }
}
