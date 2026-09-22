package com.weeth.domain.penalty.application.usecase.query

import com.weeth.domain.club.domain.repository.ClubReader
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.penalty.application.dto.response.PenaltyRuleResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetPenaltyRuleQueryService(
    private val clubReader: ClubReader,
    private val clubMemberPolicy: ClubMemberPolicy,
) {
    fun getRule(
        clubId: Long,
        userId: Long,
    ): PenaltyRuleResponse {
        clubMemberPolicy.getActiveMember(clubId, userId)
        val club = clubReader.getClubById(clubId)
        return PenaltyRuleResponse(content = club.penaltyRule)
    }
}
