package com.weeth.domain.cardinal.application.mapper

import com.weeth.domain.cardinal.application.dto.request.CardinalSaveRequest
import com.weeth.domain.cardinal.application.dto.response.CardinalResponse
import com.weeth.domain.cardinal.domain.entity.Cardinal
import com.weeth.domain.club.domain.entity.Club
import org.springframework.stereotype.Component

@Component
class CardinalMapper {
    fun toEntity(
        club: Club,
        request: CardinalSaveRequest,
    ): Cardinal =
        Cardinal.create(
            club = club,
            cardinalNumber = request.cardinalNumber,
        )

    fun toResponse(cardinal: Cardinal): CardinalResponse =
        CardinalResponse(
            cardinal.id,
            cardinal.cardinalNumber,
            cardinal.status,
            cardinal.createdAt,
            cardinal.modifiedAt,
        )
}
