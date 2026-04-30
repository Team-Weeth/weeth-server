package com.weeth.domain.cardinal.application.usecase.query

import com.weeth.domain.cardinal.application.dto.response.CardinalResponse
import com.weeth.domain.cardinal.application.mapper.CardinalMapper
import com.weeth.domain.cardinal.domain.repository.CardinalReader
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetCardinalQueryService(
    private val cardinalReader: CardinalReader,
    private val clubMemberPolicy: ClubMemberPolicy,
    private val cardinalMapper: CardinalMapper,
) {
    fun findAll(
        clubId: Long,
        userId: Long,
    ): List<CardinalResponse> {
        clubMemberPolicy.getActiveMember(clubId, userId)

        return cardinalReader.findAllByClubIdOrderByCardinalNumberAsc(clubId).map(cardinalMapper::toResponse)
    }
}
