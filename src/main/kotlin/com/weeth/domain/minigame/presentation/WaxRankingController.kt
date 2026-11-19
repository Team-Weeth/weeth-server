package com.weeth.domain.minigame.presentation

import com.weeth.domain.minigame.application.dto.response.WaxRankingResponse
import com.weeth.domain.minigame.application.usecase.query.GetWaxRankingQueryService
import com.weeth.domain.minigame.domain.vo.WaxRankingPeriod
import com.weeth.domain.minigame.presentation.MinigameResponseCode.WAX_RANKING_FIND_SUCCESS
import com.weeth.global.common.exception.ApiErrorCodeExample
import com.weeth.global.common.response.CommonResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "MINIGAME", description = "미니게임 API")
@RestController
@RequestMapping("/api/v4/minigame/wax")
@ApiErrorCodeExample
class WaxRankingController(
    private val getWaxRankingQueryService: GetWaxRankingQueryService,
) {
    @GetMapping("/rankings")
    @Operation(summary = "왁뿌볼 일간·주간 랭킹 조회 (로그인 불필요)")
    @SecurityRequirements
    fun getRanking(
        @Parameter(description = "집계 기간 (DAILY: 오늘, WEEKLY: 이번 주 월요일부터)", example = "DAILY")
        @RequestParam period: WaxRankingPeriod,
        @Parameter(description = "게임 입장 때 보낸 기기 토큰. 있으면 내 순위를 함께 돌려준다")
        @RequestHeader(name = TOKEN_HEADER, required = false) token: String?,
    ): CommonResponse<WaxRankingResponse> =
        CommonResponse.success(WAX_RANKING_FIND_SUCCESS, getWaxRankingQueryService.getRanking(period, token))

    companion object {
        const val TOKEN_HEADER = "X-Wax-Token"
    }
}
