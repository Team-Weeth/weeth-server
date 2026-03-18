package com.weeth.domain.university.application.usecase.query

import com.weeth.config.CacheBenchmarkUtil
import com.weeth.config.TestContainersConfig
import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.longs.shouldBeLessThan
import org.junit.jupiter.api.Tag
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cache.CacheManager
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

/*
    [실제 CareerNet API 캐시 성능 벤치마크]
    - 실제 CareerNetAdapter를 사용하여 캐시 미스/히트 성능 차이를 측정합니다.
    - 20번 호출 시 캐싱 없을 때(20 × API)와 캐싱 있을 때(1 × API + 19 × Redis) 비교합니다.
    - CAREER_NET_API_KEY 환경변수가 없으면 전체 테스트를 스킵합니다.
    - 실행: export $(cat .env | xargs) && ./gradlew test --tests "UniversityRealApiCacheTest"
 */
@Tag("performance")
@OptIn(ExperimentalKotest::class)
@SpringBootTest
@ActiveProfiles("test")
@Import(TestContainersConfig::class)
class UniversityRealApiCacheTest(
    private val getUniversityQueryService: GetUniversityQueryService,
    private val cacheManager: CacheManager,
) : DescribeSpec({

        val hasRealApiKey = System.getenv("CAREER_NET_API_KEY") != null

        beforeEach {
            cacheManager.getCache("schools")?.clear()
            cacheManager.getCache("majors")?.clear()
        }

        describe("getSchools - 실제 API").config(enabled = hasRealApiKey) {
            it("캐싱 없이 20번 vs 캐싱 있을 때 20번 성능 비교") {
                val result =
                    CacheBenchmarkUtil.benchmarkRounds(
                        cacheName = "schools",
                        rounds = 20,
                        clearCache = { cacheManager.getCache("schools")?.clear() },
                    ) {
                        getUniversityQueryService.getSchools()
                    }
                println(result)

                result.totalWithCacheMs shouldBeLessThan result.estimatedTotalWithoutCacheMs
            }
        }

        describe("getMajors - 실제 API").config(enabled = hasRealApiKey) {
            it("캐싱 없이 20번 vs 캐싱 있을 때 20번 성능 비교") {
                val result =
                    CacheBenchmarkUtil.benchmarkRounds(
                        cacheName = "majors",
                        rounds = 20,
                        clearCache = { cacheManager.getCache("majors")?.clear() },
                    ) {
                        getUniversityQueryService.getMajors()
                    }
                println(result)

                result.totalWithCacheMs shouldBeLessThan result.estimatedTotalWithoutCacheMs
            }
        }
    })
