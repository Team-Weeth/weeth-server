package com.weeth.domain.cardinal.application.usecase.command

import com.weeth.domain.cardinal.application.dto.request.CardinalSaveRequest
import com.weeth.domain.cardinal.application.dto.request.CardinalUpdateRequest
import com.weeth.domain.cardinal.application.exception.CardinalNotFoundException
import com.weeth.domain.cardinal.application.exception.DuplicateCardinalException
import com.weeth.domain.cardinal.application.mapper.CardinalMapper
import com.weeth.domain.cardinal.domain.repository.CardinalRepository
import com.weeth.domain.cardinal.domain.service.CardinalStatusPolicy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ManageCardinalUseCase(
    private val cardinalRepository: CardinalRepository,
    private val cardinalMapper: CardinalMapper,
    private val cardinalStatusPolicy: CardinalStatusPolicy,
) {
    @Transactional
    fun save(request: CardinalSaveRequest) {
        if (cardinalRepository.findByCardinalNumber(request.cardinalNumber).isPresent) {
            throw DuplicateCardinalException()
        }

        val cardinal = cardinalRepository.save(cardinalMapper.toEntity(request))
        if (request.inProgress) {
            cardinalStatusPolicy.activateExclusively(cardinal)
        }
    }

    @Transactional
    fun update(request: CardinalUpdateRequest) {
        val cardinal = cardinalRepository.findById(request.id).orElseThrow { CardinalNotFoundException() }
        cardinal.update(request.year, request.semester)

        if (request.inProgress) {
            cardinalStatusPolicy.activateExclusively(cardinal)
        }
    }
}
