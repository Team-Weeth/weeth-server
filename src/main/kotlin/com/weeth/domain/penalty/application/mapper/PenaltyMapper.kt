package com.weeth.domain.penalty.application.mapper

import com.weeth.domain.penalty.application.dto.PenaltyDTO
import com.weeth.domain.penalty.domain.entity.Penalty
import com.weeth.domain.penalty.domain.entity.enums.PenaltyType
import com.weeth.domain.user.domain.entity.Cardinal
import com.weeth.domain.user.domain.entity.User
import com.weeth.domain.user.domain.entity.UserCardinal
import org.springframework.stereotype.Component

@Component
class PenaltyMapper {
    fun fromPenaltyDto(
        dto: PenaltyDTO.Save,
        user: User,
        cardinal: Cardinal,
    ): Penalty =
        Penalty(
            user = user,
            cardinal = cardinal,
            penaltyType = dto.penaltyType,
            penaltyDescription = dto.penaltyDescription ?: "",
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

    fun toPenaltyDto(
        user: User,
        penalties: List<PenaltyDTO.Penalties>,
        userCardinals: List<UserCardinal>,
    ): PenaltyDTO.Response =
        PenaltyDTO.Response(
            userId = user.id,
            penaltyCount = null,
            warningCount = null,
            name = null,
            cardinals = userCardinals.map { it.cardinal.cardinalNumber },
            penalties = penalties,
        )

    fun toPenalties(penalty: Penalty): PenaltyDTO.Penalties =
        PenaltyDTO.Penalties(
            penaltyId = penalty.id,
            penaltyType = penalty.penaltyType,
            cardinal = penalty.cardinal?.cardinalNumber,
            penaltyDescription = penalty.penaltyDescription,
            time = penalty.modifiedAt,
        )

    fun toResponseAll(
        cardinal: Int?,
        responses: List<PenaltyDTO.Response>,
    ): PenaltyDTO.ResponseAll =
        PenaltyDTO.ResponseAll(
            cardinal = cardinal,
            responses = responses,
        )
}
