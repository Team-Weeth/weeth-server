package com.weeth.domain.penalty.application.mapper

import com.weeth.domain.cardinal.domain.entity.Cardinal
import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.club.domain.entity.ClubMemberCardinal
import com.weeth.domain.file.domain.port.FileAccessUrlPort
import com.weeth.domain.penalty.application.dto.request.SavePenaltyRequest
import com.weeth.domain.penalty.application.dto.response.MemberPenaltyDetailResponse
import com.weeth.domain.penalty.application.dto.response.PenaltyByCardinalResponse
import com.weeth.domain.penalty.application.dto.response.PenaltyDetailResponse
import com.weeth.domain.penalty.application.dto.response.PenaltyResponse
import com.weeth.domain.penalty.domain.entity.Penalty
import org.springframework.stereotype.Component

@Component
class PenaltyMapper(
    private val fileAccessUrlPort: FileAccessUrlPort,
) {
    fun toEntity(
        request: SavePenaltyRequest,
        clubMember: ClubMember,
        cardinal: Cardinal,
    ): Penalty =
        Penalty(
            clubMember = clubMember,
            cardinal = cardinal,
            penaltyDescription = request.penaltyDescription,
            penaltyType = request.penaltyType,
            score = request.score,
        )

    fun toResponse(
        clubMember: ClubMember,
        penalties: List<Penalty>,
        clubMemberCardinals: List<ClubMemberCardinal>,
    ): PenaltyResponse =
        PenaltyResponse(
            userId = clubMember.user.id,
            name = clubMember.user.name,
            memberStatus = clubMember.memberStatus,
            penaltyCount = clubMember.penaltyCount,
            cardinals = clubMemberCardinals.map { it.cardinal.cardinalNumber },
            penalties = penalties.map(::toDetailResponse),
        )

    fun toDetailResponse(penalty: Penalty): PenaltyDetailResponse =
        PenaltyDetailResponse(
            penaltyId = penalty.id,
            cardinal = penalty.cardinal.cardinalNumber,
            penaltyDescription = penalty.penaltyDescription,
            score = penalty.score,
            time = penalty.createdAt,
        )

    fun toByCardinalResponse(
        cardinal: Int?,
        responses: List<PenaltyResponse>,
    ): PenaltyByCardinalResponse =
        PenaltyByCardinalResponse(
            cardinal = cardinal,
            responses = responses,
        )

    fun toMemberPenaltyDetailResponse(
        clubMember: ClubMember,
        cardinals: List<ClubMemberCardinal>,
        penalties: List<Penalty>,
    ): MemberPenaltyDetailResponse =
        MemberPenaltyDetailResponse(
            profileImageUrl =
                (clubMember.userProfile?.profileImageStorageKey ?: clubMember.profileImageStorageKey)
                    ?.let { fileAccessUrlPort.resolve(it) },
            name = clubMember.user.name,
            cardinals = cardinals.map { it.cardinal.cardinalNumber }.sorted(),
            memberStatus = clubMember.memberStatus,
            bio = clubMember.userProfile?.bio ?: clubMember.bio,
            penalties = penalties.map(::toDetailResponse),
        )
}
