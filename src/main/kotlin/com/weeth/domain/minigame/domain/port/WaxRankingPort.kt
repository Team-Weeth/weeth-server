package com.weeth.domain.minigame.domain.port

import com.weeth.domain.minigame.domain.vo.WaxRankEntry
import com.weeth.domain.minigame.domain.vo.WaxRankingBucket
import com.weeth.domain.minigame.domain.vo.WaxScore

/** 왁뿌볼 일간·주간 랭킹 저장소. */
interface WaxRankingPort {
    /**
     * [score]를 기록 시각이 속한 모든 기간(일간·주간)에 반영한다.
     * 점수는 기간 내 기존 기록보다 높을 때만 갱신하고, 닉네임은 항상 최근 값으로 바꾼다.
     */
    fun record(score: WaxScore)

    /** 점수 높은 순으로 상위 [limit]명. */
    fun top(
        bucket: WaxRankingBucket,
        limit: Int,
    ): List<WaxRankEntry>

    /** [tokenHash]의 순위. 기록이 없으면 null. */
    fun find(
        bucket: WaxRankingBucket,
        tokenHash: String,
    ): WaxRankEntry?
}
