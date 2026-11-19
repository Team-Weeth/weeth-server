package com.weeth.domain.minigame.presentation

import com.weeth.domain.minigame.application.dto.response.WaxRankingEntryResponse
import com.weeth.domain.minigame.application.dto.response.WaxRankingResponse
import com.weeth.domain.minigame.application.usecase.query.GetWaxRankingQueryService
import com.weeth.domain.minigame.domain.vo.WaxRankingPeriod
import com.weeth.global.common.exception.CommonExceptionHandler
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.LocalDate
import java.time.LocalDateTime

class WaxRankingControllerTest :
    DescribeSpec({
        val queryService = mockk<GetWaxRankingQueryService>()
        val mvc =
            MockMvcBuilders
                .standaloneSetup(WaxRankingController(queryService))
                .setControllerAdvice(CommonExceptionHandler())
                .build()
        val response =
            WaxRankingResponse(
                period = WaxRankingPeriod.DAILY,
                startDate = LocalDate.parse("2026-11-18"),
                resetAt = LocalDateTime.parse("2026-11-19T00:00:00"),
                rankings = listOf(WaxRankingEntryResponse(1, "복학생", 7)),
                me = WaxRankingEntryResponse(1, "복학생", 7),
            )

        beforeTest { clearMocks(queryService) }

        describe("GET /api/v4/minigame/wax/rankings") {
            it("기간과 X-Wax-Token을 넘기고 성공 코드 11600으로 랭킹을 돌려준다") {
                every { queryService.getRanking(WaxRankingPeriod.DAILY, "device-token-123456") } returns response

                mvc
                    .perform(
                        get("/api/v4/minigame/wax/rankings")
                            .param("period", "DAILY")
                            .header(WaxRankingController.TOKEN_HEADER, "device-token-123456")
                            .accept(MediaType.APPLICATION_JSON),
                    ).andExpect(status().isOk)
                    .andExpect(jsonPath("$.code").value(MinigameResponseCode.WAX_RANKING_FIND_SUCCESS.code))
                    .andExpect(jsonPath("$.data.period").value("DAILY"))
                    .andExpect(jsonPath("$.data.rankings[0].name").value("복학생"))
                    .andExpect(jsonPath("$.data.rankings[0].brokenWaxes").value(7))
                    .andExpect(jsonPath("$.data.me.rank").value(1))
            }

            it("토큰 헤더가 없으면 null로 넘긴다") {
                every { queryService.getRanking(WaxRankingPeriod.WEEKLY, null) } returns response.copy(me = null)

                mvc
                    .perform(get("/api/v4/minigame/wax/rankings").param("period", "WEEKLY"))
                    .andExpect(status().isOk)
                verify { queryService.getRanking(WaxRankingPeriod.WEEKLY, null) }
            }

            it("없는 기간이면 400을 돌려준다") {
                mvc
                    .perform(get("/api/v4/minigame/wax/rankings").param("period", "MONTHLY"))
                    .andExpect(status().isBadRequest)
            }
        }
    })
