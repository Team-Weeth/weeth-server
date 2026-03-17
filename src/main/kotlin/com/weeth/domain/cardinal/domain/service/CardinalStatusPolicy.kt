package com.weeth.domain.cardinal.domain.service

import com.weeth.domain.cardinal.domain.entity.Cardinal
import com.weeth.domain.cardinal.domain.repository.CardinalRepository
import org.springframework.stereotype.Service

@Service
class CardinalStatusPolicy(
    private val cardinalRepository: CardinalRepository,
) {
    fun activateExclusively(cardinal: Cardinal) {
        // TODO: 현재는 전역 IN_PROGRESS cardinal을 모두 종료한다. clubId 기준으로 범위를 제한해야 동아리 간 격리가 유지된다.
        val inProgressCardinals = cardinalRepository.findAllInProgressWithLock()
        inProgressCardinals.forEach(Cardinal::done)
        cardinal.inProgress()
    }
}
