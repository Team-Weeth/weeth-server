package com.weeth.domain.penalty.application.usecase.command

import com.weeth.domain.club.domain.service.ClubPermissionPolicy
import com.weeth.domain.penalty.application.dto.request.UpdatePenaltyRequest
import com.weeth.domain.penalty.application.exception.PenaltyNotFoundException
import com.weeth.domain.penalty.domain.repository.PenaltyRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UpdatePenaltyUseCase(
    private val penaltyRepository: PenaltyRepository,
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

        penalty.update(penaltyDescription = request.penaltyDescription?.takeIf { it.isNotBlank() })
    }
}
