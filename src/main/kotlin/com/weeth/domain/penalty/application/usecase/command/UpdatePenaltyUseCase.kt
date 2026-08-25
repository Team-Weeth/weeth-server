package com.weeth.domain.penalty.application.usecase.command

import com.weeth.domain.club.domain.repository.ClubMemberRepository
import com.weeth.domain.club.domain.service.ClubPermissionPolicy
import com.weeth.domain.penalty.application.dto.request.UpdatePenaltyRequest
import com.weeth.domain.penalty.application.exception.PenaltyNotFoundException
import com.weeth.domain.penalty.domain.enums.PenaltyType
import com.weeth.domain.penalty.domain.repository.PenaltyRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UpdatePenaltyUseCase(
    private val penaltyRepository: PenaltyRepository,
    private val clubMemberRepository: ClubMemberRepository,
    private val clubPermissionPolicy: ClubPermissionPolicy,
) {
    @Transactional
    fun update(
        clubId: Long,
        userId: Long,
        request: UpdatePenaltyRequest,
    ) {
        clubPermissionPolicy.requireAdmin(clubId, userId)

        val penalty =
            penaltyRepository.findByIdWithLock(request.penaltyId)
                ?: throw PenaltyNotFoundException()
        if (penalty.clubMember.club.id != clubId) throw PenaltyNotFoundException()

        val oldScore = penalty.score
        penalty.update(
            penaltyDescription = request.penaltyDescription?.takeIf { it.isNotBlank() },
            score = request.score,
        )

        if (request.score != null && request.score != oldScore) {
            val lockedMember =
                clubMemberRepository.findByIdWithLock(penalty.clubMember.id)
                    ?: throw PenaltyNotFoundException()
            val delta = request.score - oldScore
            when (penalty.penaltyType) {
                PenaltyType.PENALTY -> lockedMember.adjustPenaltyCount(delta)
                PenaltyType.WARNING -> lockedMember.adjustWarningCount(delta)
            }
        }
    }
}
