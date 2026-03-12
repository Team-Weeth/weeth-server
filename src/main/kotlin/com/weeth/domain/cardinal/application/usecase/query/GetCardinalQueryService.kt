package com.weeth.domain.cardinal.application.usecase.query

import com.weeth.domain.cardinal.application.dto.response.CardinalResponse
import com.weeth.domain.cardinal.application.mapper.CardinalMapper
import com.weeth.domain.cardinal.domain.repository.CardinalReader
import com.weeth.domain.cardinal.domain.repository.CardinalRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetCardinalQueryService(
    private val cardinalRepository: CardinalRepository,
    private val cardinalReader: CardinalReader,
    private val cardinalMapper: CardinalMapper,
) {
    // TODO(PR4): 해당 클럽 소속 멤버인지 검증 필요
    fun findAll(clubId: Long): List<CardinalResponse> =
        cardinalReader.findAllByClubIdOrderByCardinalNumberAsc(clubId).map(cardinalMapper::toResponse)
}
