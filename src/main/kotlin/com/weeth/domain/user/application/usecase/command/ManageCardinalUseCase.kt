package com.weeth.domain.user.application.usecase.command

import com.weeth.domain.user.application.dto.request.CardinalSaveRequest
import com.weeth.domain.user.application.dto.request.CardinalUpdateRequest
import com.weeth.domain.user.application.exception.CardinalNotFoundException
import com.weeth.domain.user.application.exception.DuplicateCardinalException
import com.weeth.domain.user.application.mapper.CardinalMapper
import com.weeth.domain.user.domain.entity.Cardinal
import com.weeth.domain.user.domain.entity.enums.CardinalStatus
import com.weeth.domain.user.domain.repository.CardinalRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ManageCardinalUseCase(
    private val cardinalRepository: CardinalRepository,
    private val cardinalMapper: CardinalMapper,
) {
    @Transactional
    fun save(request: CardinalSaveRequest) {
        if (cardinalRepository.findByCardinalNumber(request.cardinalNumber).isPresent) {
            throw DuplicateCardinalException()
        }

        val cardinal = cardinalRepository.save(cardinalMapper.toEntity(request))
        if (request.inProgress) {
            updateCardinalStatus(cardinal)
        }
    }

    @Transactional
    fun update(request: CardinalUpdateRequest) {
        val cardinal = cardinalRepository.findById(request.id).orElseThrow { CardinalNotFoundException() }
        cardinal.update(request.year, request.semester)

        if (request.inProgress) {
            updateCardinalStatus(cardinal)
        }
    }

    private fun updateCardinalStatus(cardinal: Cardinal) {
        val inProgressCardinals = cardinalRepository.findAllByStatus(CardinalStatus.IN_PROGRESS)
        inProgressCardinals.forEach(Cardinal::done)
        cardinal.inProgress()
    }
}
