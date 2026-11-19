package com.weeth.domain.minigame.application.mapper

import com.weeth.domain.minigame.application.dto.response.WaxRankingEntryResponse
import com.weeth.domain.minigame.application.dto.response.WaxRankingResponse
import com.weeth.domain.minigame.domain.vo.WaxRankEntry
import com.weeth.domain.minigame.domain.vo.WaxRankingBucket
import com.weeth.domain.minigame.domain.vo.WaxRankingPeriod
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class WaxRankingMapper {
    fun toResponse(
        bucket: WaxRankingBucket,
        rankings: List<WaxRankEntry>,
        me: WaxRankEntry?,
    ) = WaxRankingResponse(
        period = bucket.period,
        startDate = bucket.startDate,
        resetAt = LocalDateTime.ofInstant(bucket.endsAt, WaxRankingPeriod.ZONE),
        rankings = rankings.map(::toResponse),
        me = me?.let(::toResponse),
    )

    fun toResponse(entry: WaxRankEntry) =
        WaxRankingEntryResponse(rank = entry.rank, name = entry.name, brokenWaxes = entry.brokenWaxes)
}
