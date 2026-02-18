package com.weeth.domain.penalty.application.mapper

import com.weeth.domain.penalty.application.dto.request.SavePenaltyRequest
import com.weeth.domain.penalty.application.dto.response.PenaltyByCardinalResponse
import com.weeth.domain.penalty.application.dto.response.PenaltyDetailResponse
import com.weeth.domain.penalty.application.dto.response.PenaltyResponse
import com.weeth.domain.penalty.domain.entity.Penalty
import com.weeth.domain.penalty.domain.enums.PenaltyType
import com.weeth.domain.user.domain.entity.Cardinal
import com.weeth.domain.user.domain.entity.User
import com.weeth.domain.user.domain.entity.UserCardinal
import org.springframework.stereotype.Component

@Component
class PenaltyMapper {
    fun toEntity(
        request: SavePenaltyRequest,
        user: User,
        cardinal: Cardinal,
    ): Penalty =
        Penalty(
            user = user,
            cardinal = cardinal,
            penaltyType = request.penaltyType,
            penaltyDescription = request.penaltyDescription ?: "",
        )

    fun toAutoPenalty(
        penaltyDescription: String,
        user: User,
        cardinal: Cardinal,
        penaltyType: PenaltyType,
    ): Penalty =
        Penalty(
            user = user,
            cardinal = cardinal,
            penaltyType = penaltyType,
            penaltyDescription = penaltyDescription,
        )

    fun toResponse(
        user: User,
        penalties: List<PenaltyDetailResponse>,
        userCardinals: List<UserCardinal>,
    ): PenaltyResponse =
        PenaltyResponse(
            userId = user.id,
            penaltyCount = null,
            warningCount = null,
            name = null,
            cardinals = userCardinals.map { it.cardinal.cardinalNumber },
            penalties = penalties,
        )

    fun toDetailResponse(penalty: Penalty): PenaltyDetailResponse =
        PenaltyDetailResponse(
            penaltyId = penalty.id,
            penaltyType = penalty.penaltyType,
            cardinal = penalty.cardinal?.cardinalNumber,
            penaltyDescription = penalty.penaltyDescription,
            time = penalty.modifiedAt,
        )

    fun toByCardinalResponse(
        cardinal: Int?,
        responses: List<PenaltyResponse>,
    ): PenaltyByCardinalResponse =
        PenaltyByCardinalResponse(
            cardinal = cardinal,
            responses = responses,
        )
}
