package com.weeth.domain.minigame.application.usecase.query

import com.weeth.domain.minigame.application.dto.response.WaxRankingResponse
import com.weeth.domain.minigame.application.mapper.WaxRankingMapper
import com.weeth.domain.minigame.domain.port.WaxRankingPort
import com.weeth.domain.minigame.domain.vo.WaxPlayerToken
import com.weeth.domain.minigame.domain.vo.WaxRankingPeriod
import org.springframework.stereotype.Service
import java.time.Clock

/** 랭킹은 Redis에만 있어 DB 트랜잭션(커넥션)을 열지 않는다. */
@Service
class GetWaxRankingQueryService(
    private val rankingPort: WaxRankingPort,
    private val waxRankingMapper: WaxRankingMapper,
    private val clock: Clock,
) {
    fun getRanking(
        period: WaxRankingPeriod,
        token: String?,
    ): WaxRankingResponse {
        val bucket = period.bucketOf(clock.instant())
        val rankings = rankingPort.top(bucket, TOP_LIMIT)
        // 토큰 형식이 틀리면 내 순위만 빼고 돌려준다.
        val me = WaxPlayerToken.parseOrNull(token)?.let { rankingPort.find(bucket, it.hash()) }
        return waxRankingMapper.toResponse(bucket, rankings, me)
    }

    companion object {
        const val TOP_LIMIT = 10
    }
}
