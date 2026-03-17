package com.weeth.domain.penalty.application.mapper

import com.weeth.domain.cardinal.domain.entity.Cardinal
import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.club.domain.entity.ClubMemberCardinal
import com.weeth.domain.penalty.application.dto.request.SavePenaltyRequest
import com.weeth.domain.penalty.application.dto.response.PenaltyByCardinalResponse
import com.weeth.domain.penalty.application.dto.response.PenaltyDetailResponse
import com.weeth.domain.penalty.application.dto.response.PenaltyResponse
import com.weeth.domain.penalty.domain.entity.Penalty
import com.weeth.domain.penalty.domain.enums.PenaltyType
import org.springframework.stereotype.Component

@Component
class PenaltyMapper {
    fun toEntity(
        request: SavePenaltyRequest,
        clubMember: ClubMember,
        cardinal: Cardinal,
    ): Penalty =
        Penalty(
            clubMember = clubMember,
            cardinal = cardinal,
            penaltyType = request.penaltyType,
            penaltyDescription = request.penaltyDescription ?: "",
        )

    fun toAutoPenalty(
        penaltyDescription: String,
        clubMember: ClubMember,
        cardinal: Cardinal,
    ): Penalty =
        Penalty(
            clubMember = clubMember,
            cardinal = cardinal,
            penaltyType = PenaltyType.AUTO_PENALTY,
            penaltyDescription = penaltyDescription,
        )

    fun toResponse(
        clubMember: ClubMember,
        penalties: List<Penalty>,
        clubMemberCardinals: List<ClubMemberCardinal>,
    ): PenaltyResponse =
        PenaltyResponse(
            userId = clubMember.user.id,
            name = clubMember.user.name,
            penaltyCount = clubMember.penaltyCount,
            cardinals = clubMemberCardinals.map { it.cardinal.cardinalNumber },
            penalties = penalties.map(::toDetailResponse),
        )

    fun toDetailResponse(penalty: Penalty): PenaltyDetailResponse =
        PenaltyDetailResponse(
            penaltyId = penalty.id,
            penaltyType = penalty.penaltyType,
            cardinal = penalty.cardinal.cardinalNumber,
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
