package com.weeth.domain.club.application.mapper

import com.weeth.domain.club.application.dto.response.ClubPositionOptionResponse
import com.weeth.domain.club.domain.entity.ClubPositionOption
import org.springframework.stereotype.Component

@Component
class ClubPositionOptionMapper {
    fun toResponse(option: ClubPositionOption) =
        ClubPositionOptionResponse(
            id = option.id,
            name = option.name,
            colorHex = option.colorHex,
            displayOrder = option.displayOrder,
        )

    fun toResponses(options: List<ClubPositionOption>): List<ClubPositionOptionResponse> =
        options.map { toResponse(it) }
}
