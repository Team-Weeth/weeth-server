package com.weeth.domain.university.application.usecase.query

import com.ninjasquad.springmockk.MockkBean
import com.weeth.config.TestContainersConfig
import com.weeth.domain.university.domain.model.MajorData
import com.weeth.domain.university.domain.model.SchoolData
import com.weeth.domain.university.domain.port.UniversityInfoPort
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.verify
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cache.CacheManager
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

/*
    [캐시 통합 테스트]
    - 실제 Redis(Testcontainers)와 Spring Cache AOP를 사용하여 캐시 동작을 검증합니다.
    - UniversityInfoPort는 Mock으로 대체하여 실제 CareerNet API를 호출하지 않습니다.
    - 각 테스트 전 캐시를 초기화하여 테스트 간 간섭을 방지합니다.
    - 성능 벤치마크는 UniversityRealApiCacheTest에서 실제 API를 사용해 측정합니다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestContainersConfig::class)
class UniversityCacheIntegrationTest(
    private val getUniversityQueryService: GetUniversityQueryService,
    @MockkBean private val universityInfoPort: UniversityInfoPort,
    private val cacheManager: CacheManager,
) : DescribeSpec({

        val schoolData = (1..20).map { SchoolData("학교$it", "서울") }
        val majorData = (1..20).map { MajorData("학과$it", "공학계열") }

        beforeEach {
            clearMocks(universityInfoPort)
            cacheManager.getCache("schools")?.clear()
            cacheManager.getCache("majors")?.clear()
        }

        describe("getSchools") {
            context("캐시 미스") {
                it("UniversityInfoPort를 1번 호출하고 결과를 반환한다") {
                    every { universityInfoPort.getSchools() } returns schoolData

                    val result = getUniversityQueryService.getSchools()

                    result shouldHaveSize 20
                    verify(exactly = 1) { universityInfoPort.getSchools() }
                }
            }

            context("캐시 히트") {
                it("두 번 호출해도 포트는 1번만 호출된다") {
                    every { universityInfoPort.getSchools() } returns schoolData

                    getUniversityQueryService.getSchools() // cache miss
                    getUniversityQueryService.getSchools() // cache hit

                    verify(exactly = 1) { universityInfoPort.getSchools() }
                }

                it("캐시에서 반환한 결과가 포트 응답과 동일하다") {
                    every { universityInfoPort.getSchools() } returns schoolData

                    val first = getUniversityQueryService.getSchools()
                    val second = getUniversityQueryService.getSchools()

                    first shouldBe second
                }
            }
        }

        describe("getMajors") {
            context("캐시 미스") {
                it("UniversityInfoPort를 1번 호출하고 결과를 반환한다") {
                    every { universityInfoPort.getMajors() } returns majorData

                    val result = getUniversityQueryService.getMajors()

                    result shouldHaveSize 20
                    verify(exactly = 1) { universityInfoPort.getMajors() }
                }
            }

            context("캐시 히트") {
                it("두 번 호출해도 포트는 1번만 호출된다") {
                    every { universityInfoPort.getMajors() } returns majorData

                    getUniversityQueryService.getMajors() // cache miss
                    getUniversityQueryService.getMajors() // cache hit

                    verify(exactly = 1) { universityInfoPort.getMajors() }
                }

                it("캐시에서 반환한 결과가 포트 응답과 동일하다") {
                    every { universityInfoPort.getMajors() } returns majorData

                    val first = getUniversityQueryService.getMajors()
                    val second = getUniversityQueryService.getMajors()

                    first shouldBe second
                }
            }
        }
    })
