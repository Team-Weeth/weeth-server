package com.weeth.domain.cardinal.domain.service

import com.weeth.domain.cardinal.domain.entity.Cardinal
import com.weeth.domain.cardinal.domain.repository.CardinalRepository
import org.springframework.stereotype.Service

@Service
class CardinalStatusPolicy(
    private val cardinalRepository: CardinalRepository,
) {
    fun activateExclusively(cardinal: Cardinal) {
        val inProgressCardinals = cardinalRepository.findAllInProgressByClubIdWithLock(cardinal.club.id)
        inProgressCardinals.forEach(Cardinal::done)
        cardinal.inProgress()
    }
}
