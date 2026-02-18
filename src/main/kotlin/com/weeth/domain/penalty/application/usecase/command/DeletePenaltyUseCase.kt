package com.weeth.domain.penalty.application.usecase.command

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
) {
    @Transactional
    fun execute(penaltyId: Long) {
        val penalty =
            penaltyRepository.findByIdOrNull(penaltyId)
                ?: throw PenaltyNotFoundException()

        if (penalty.penaltyType == PenaltyType.AUTO_PENALTY) {
            throw AutoPenaltyDeleteNotAllowedException()
        }

        val user = penalty.user

        when (penalty.penaltyType) {
            PenaltyType.PENALTY -> {
                user.decrementPenaltyCount()
            }

            PenaltyType.WARNING -> {
                if (user.warningCount % 2 == 0) {
                    val relatedAutoPenalty =
                        penaltyRepository
                            .findFirstByUserAndCardinalAndPenaltyTypeAndCreatedAtAfterOrderByCreatedAtAsc(
                                penalty.user,
                                penalty.cardinal,
                                PenaltyType.AUTO_PENALTY,
                                penalty.createdAt,
                            )
                    if (relatedAutoPenalty != null) {
                        penaltyRepository.deleteById(relatedAutoPenalty.id)
                    }
                    user.decrementPenaltyCount()
                }
                user.decrementWarningCount()
            }

            else -> {}
        }

        penaltyRepository.deleteById(penaltyId)
    }
}
