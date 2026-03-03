package com.weeth.domain.cardinal.application.mapper

import com.weeth.domain.cardinal.application.dto.request.CardinalSaveRequest
import com.weeth.domain.cardinal.application.dto.response.CardinalResponse
import com.weeth.domain.cardinal.domain.entity.Cardinal
import org.springframework.stereotype.Component

@Component
class CardinalMapper {
    fun toEntity(request: CardinalSaveRequest): Cardinal =
        Cardinal.create(
            cardinalNumber = request.cardinalNumber,
            year = request.year,
            semester = request.semester,
        )

    fun toResponse(cardinal: Cardinal): CardinalResponse =
        CardinalResponse(
            cardinal.id,
            cardinal.cardinalNumber,
            cardinal.year,
            cardinal.semester,
            cardinal.status,
            cardinal.createdAt,
            cardinal.modifiedAt,
        )
}
