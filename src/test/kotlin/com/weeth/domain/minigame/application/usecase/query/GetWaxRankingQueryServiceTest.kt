package com.weeth.domain.minigame.application.usecase.query

import com.weeth.domain.minigame.application.dto.response.WaxRankingEntryResponse
import com.weeth.domain.minigame.application.mapper.WaxRankingMapper
import com.weeth.domain.minigame.domain.port.WaxRankingPort
import com.weeth.domain.minigame.domain.vo.WaxPlayerToken
import com.weeth.domain.minigame.domain.vo.WaxRankEntry
import com.weeth.domain.minigame.domain.vo.WaxRankingPeriod
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class GetWaxRankingQueryServiceTest :
    DescribeSpec({
        val kst = ZoneId.of("Asia/Seoul")
        val now = LocalDateTime.parse("2026-11-18T12:00:00").atZone(kst).toInstant()
        val rankingPort = mockk<WaxRankingPort>()
        val service = GetWaxRankingQueryService(rankingPort, WaxRankingMapper(), Clock.fixed(now, kst))
        val token = WaxPlayerToken("3f2b8c1e-6a4d-4e2b-9c1f-7d8e9a0b1c2d")
        val top = listOf(WaxRankEntry(1, "복학생", 7), WaxRankEntry(2, "새내기", 3))

        beforeTest {
            clearMocks(rankingPort)
            every { rankingPort.top(any(), any()) } returns top
            every { rankingPort.find(any(), any()) } returns WaxRankEntry(2, "새내기", 3)
        }

        describe("getRanking") {
            it("지금 시각이 속한 주간 기간의 상위 10명과 기간 정보를 돌려준다") {
                val response = service.getRanking(WaxRankingPeriod.WEEKLY, null)

                response.period shouldBe WaxRankingPeriod.WEEKLY
                response.startDate shouldBe LocalDate.parse("2026-11-16")
                response.resetAt shouldBe LocalDateTime.parse("2026-11-23T00:00:00")
                response.rankings shouldBe
                    listOf(WaxRankingEntryResponse(1, "복학생", 7), WaxRankingEntryResponse(2, "새내기", 3))
                response.me shouldBe null
                verify { rankingPort.top(WaxRankingPeriod.WEEKLY.bucketOf(now), GetWaxRankingQueryService.TOP_LIMIT) }
                verify(exactly = 0) { rankingPort.find(any(), any()) }
            }

            it("유효한 토큰이면 토큰 해시로 내 순위를 찾는다") {
                val response = service.getRanking(WaxRankingPeriod.DAILY, token.value)

                response.me shouldBe WaxRankingEntryResponse(2, "새내기", 3)
                verify { rankingPort.find(WaxRankingPeriod.DAILY.bucketOf(now), token.hash()) }
            }

            it("토큰 형식이 틀리면 내 순위 없이 돌려준다") {
                service.getRanking(WaxRankingPeriod.DAILY, "bad").me shouldBe null
                verify(exactly = 0) { rankingPort.find(any(), any()) }
            }
        }
    })
