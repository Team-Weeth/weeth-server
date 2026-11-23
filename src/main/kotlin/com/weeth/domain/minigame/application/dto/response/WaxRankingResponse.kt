package com.weeth.domain.minigame.application.dto.response

import com.weeth.domain.minigame.domain.vo.WaxRankingPeriod
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime

data class WaxRankingResponse(
    @field:Schema(description = "집계 기간", example = "DAILY")
    val period: WaxRankingPeriod,
    @field:Schema(description = "기간 시작일(KST)", example = "2026-11-18")
    val startDate: LocalDate,
    @field:Schema(description = "다음 기간이 시작되는 시각(KST)", example = "2026-11-19T00:00:00")
    val resetAt: LocalDateTime,
    @field:Schema(description = "점수 높은 순 상위 랭킹")
    val rankings: List<WaxRankingEntryResponse>,
    @field:Schema(description = "내 순위. X-Wax-Token이 없거나 이번 기간 기록이 없으면 null", nullable = true)
    val me: WaxRankingEntryResponse? = null,
)

data class WaxRankingEntryResponse(
    @field:Schema(description = "순위(1부터)", example = "1")
    val rank: Int,
    @field:Schema(description = "닉네임(가장 최근에 쓴 이름)", example = "수상한 복학생")
    val name: String,
    @field:Schema(description = "한 판에 깬 왁스 수의 기간 내 최고 기록", example = "12")
    val brokenWaxes: Int,
)
