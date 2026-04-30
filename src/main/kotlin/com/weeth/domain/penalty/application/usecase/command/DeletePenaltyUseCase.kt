package com.weeth.domain.penalty.application.usecase.command

import com.weeth.domain.club.domain.repository.ClubMemberRepository
import com.weeth.domain.club.domain.service.ClubPermissionPolicy
import com.weeth.domain.penalty.application.exception.AutoPenaltyDeleteNotAllowedException
import com.weeth.domain.penalty.application.exception.PenaltyNotFoundException
import com.weeth.domain.penalty.domain.repository.PenaltyRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DeletePenaltyUseCase(
    private val penaltyRepository: PenaltyRepository,
    private val clubMemberRepository: ClubMemberRepository,
    private val clubPermissionPolicy: ClubPermissionPolicy,
) {
    @Transactional
    fun delete(
        clubId: Long,
        userId: Long,
        penaltyId: Long,
    ) {
        clubPermissionPolicy.requireAdmin(clubId, userId)

        val penalty =
            penaltyRepository.findByIdWithLock(penaltyId)
                ?: throw PenaltyNotFoundException()
        if (penalty.clubMember.club.id != clubId) throw PenaltyNotFoundException()

        val lockedMember =
            clubMemberRepository.findByIdWithLock(penalty.clubMember.id)
                ?: throw PenaltyNotFoundException()
        lockedMember.decrementPenaltyCount()

        penaltyRepository.delete(penalty)
    }
}
