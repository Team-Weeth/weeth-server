package com.weeth.domain.penalty.application.usecase.command

import com.weeth.domain.club.domain.repository.ClubRepository
import com.weeth.domain.club.domain.service.ClubPermissionPolicy
import com.weeth.domain.penalty.application.dto.request.SavePenaltyRuleRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SavePenaltyRuleUseCase(
    private val clubRepository: ClubRepository,
    private val clubPermissionPolicy: ClubPermissionPolicy,
) {
    @Transactional
    fun save(
        clubId: Long,
        userId: Long,
        request: SavePenaltyRuleRequest,
    ) {
        clubPermissionPolicy.requireAdmin(clubId, userId)
        val club = clubRepository.getClubById(clubId)
        club.updatePenaltyRule(request.content)
    }
}
