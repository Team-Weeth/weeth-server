package com.weeth.domain.penalty.application.usecase.command

import com.weeth.domain.club.domain.repository.ClubMemberRepository
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.penalty.application.exception.AutoPenaltyDeleteNotAllowedException
import com.weeth.domain.penalty.application.exception.PenaltyNotFoundException
import com.weeth.domain.penalty.domain.enums.PenaltyType
import com.weeth.domain.penalty.domain.repository.PenaltyRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DeletePenaltyUseCase(
    private val penaltyRepository: PenaltyRepository,
    private val clubMemberRepository: ClubMemberRepository,
    private val clubMemberPolicy: ClubMemberPolicy,
) {
    @Transactional
    fun delete(
        clubId: Long,
        userId: Long,
        penaltyId: Long,
    ) {
        clubMemberPolicy.requireAdmin(clubId, userId)

        val penalty =
            penaltyRepository.findByIdOrNull(penaltyId)
                ?: throw PenaltyNotFoundException()
        if (penalty.clubMember.club.id != clubId) throw PenaltyNotFoundException()

        if (penalty.penaltyType == PenaltyType.AUTO_PENALTY) {
            throw AutoPenaltyDeleteNotAllowedException()
        }

        if (penalty.penaltyType == PenaltyType.PENALTY) {
            val lockedMember =
                clubMemberRepository.findByIdWithLock(penalty.clubMember.id)
                    ?: throw PenaltyNotFoundException()
            lockedMember.decrementPenaltyCount()
        }

        penaltyRepository.delete(penalty)
    }
}
