package com.weeth.domain.penalty.application.usecase.query

import com.weeth.domain.cardinal.domain.repository.CardinalReader
import com.weeth.domain.club.domain.repository.ClubMemberCardinalReader
import com.weeth.domain.club.domain.service.ClubMemberCardinalPolicy
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.penalty.application.dto.response.PenaltyByCardinalResponse
import com.weeth.domain.penalty.application.dto.response.PenaltyResponse
import com.weeth.domain.penalty.application.mapper.PenaltyMapper
import com.weeth.domain.penalty.domain.repository.PenaltyRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetPenaltyQueryService(
    private val penaltyRepository: PenaltyRepository,
    private val clubMemberCardinalReader: ClubMemberCardinalReader,
    private val clubMemberPolicy: ClubMemberPolicy,
    private val clubMemberCardinalPolicy: ClubMemberCardinalPolicy,
    private val cardinalReader: CardinalReader,
    private val mapper: PenaltyMapper,
) {
    fun findAllByCardinal(
        clubId: Long,
        userId: Long,
        cardinalNumber: Int?,
    ): List<PenaltyByCardinalResponse> {
        clubMemberPolicy.requireAdmin(clubId, userId)
        val cardinals =
            if (cardinalNumber == null) {
                cardinalReader.findAllByClubIdOrderByCardinalNumberAsc(clubId)
            } else {
                listOf(cardinalReader.findByClubIdAndCardinalNumber(clubId, cardinalNumber) ?: return emptyList())
            }

        return cardinals.map { cardinal ->
            val penalties = penaltyRepository.findByClubIdAndCardinalIdOrderByIdDesc(clubId, cardinal.id)
            val clubMembers = penalties.map { it.clubMember }.distinct()
            val memberCardinalsMap =
                clubMemberCardinalReader
                    .findAllByClubMembers(
                        clubMembers,
                    ).groupBy { it.clubMember.id }

            val responses =
                penalties
                    .groupBy { it.clubMember.id }
                    .entries
                    .map { (clubMemberId, memberPenalties) ->
                        val clubMember = memberPenalties.first().clubMember
                        val memberCardinals = memberCardinalsMap[clubMemberId] ?: emptyList()
                        mapper.toResponse(clubMember, memberPenalties, memberCardinals)
                    }.sortedBy { it.userId }

            mapper.toByCardinalResponse(cardinal.cardinalNumber, responses)
        }
    }

    fun findByUser(
        clubId: Long,
        userId: Long,
    ): PenaltyResponse {
        val clubMember = clubMemberPolicy.getActiveMember(clubId, userId)
        val currentCardinal = clubMemberCardinalPolicy.getCurrentCardinal(clubMember)
        val penalties =
            penaltyRepository.findByClubMemberIdAndCardinalIdOrderByIdDesc(
                clubMember.id,
                currentCardinal.id,
            )
        val clubMemberCardinals = clubMemberCardinalReader.findAllByClubMember(clubMember)

        return mapper.toResponse(clubMember, penalties, clubMemberCardinals)
    }
}
