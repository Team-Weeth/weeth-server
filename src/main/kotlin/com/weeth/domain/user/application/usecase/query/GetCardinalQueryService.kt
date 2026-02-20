package com.weeth.domain.user.application.usecase.query

import com.weeth.domain.user.application.dto.response.CardinalResponse
import com.weeth.domain.user.application.mapper.CardinalMapper
import com.weeth.domain.user.domain.repository.CardinalRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetCardinalQueryService(
    private val cardinalRepository: CardinalRepository,
    private val cardinalMapper: CardinalMapper,
) {
    fun findAll(): List<CardinalResponse> = cardinalRepository.findAllByOrderByCardinalNumberAsc().map(cardinalMapper::toResponse)
}
