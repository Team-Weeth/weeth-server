package com.weeth.domain.penalty.application.usecase.command

import com.weeth.domain.penalty.application.exception.AutoPenaltyDeleteNotAllowedException
import com.weeth.domain.penalty.application.exception.PenaltyNotFoundException
import com.weeth.domain.penalty.domain.enums.PenaltyType
import com.weeth.domain.penalty.domain.repository.PenaltyRepository
import com.weeth.domain.user.application.exception.UserNotFoundException
import com.weeth.domain.user.domain.repository.UserRepository
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DeletePenaltyUseCase(
    private val penaltyRepository: PenaltyRepository,
    private val userRepository: UserRepository,
) {
    @Transactional
    fun delete(penaltyId: Long) {
        val penalty =
            penaltyRepository.findByIdOrNull(penaltyId)
                ?: throw PenaltyNotFoundException()

        if (penalty.penaltyType == PenaltyType.AUTO_PENALTY) {
            throw AutoPenaltyDeleteNotAllowedException()
        }

        val user =
            userRepository
                .findByIdWithLock(penalty.user.id)
                .orElseThrow { UserNotFoundException() }

        when (penalty.penaltyType) {
            PenaltyType.PENALTY -> {
                user.decrementPenaltyCount()
            }

            PenaltyType.WARNING -> {
                if (user.warningCount % 2 == 0) {
                    val relatedAutoPenalty =
                        penaltyRepository
                            .findFirstAutoPenaltyAfter(
                                penalty.user.id,
                                penalty.cardinal.id,
                                PenaltyType.AUTO_PENALTY,
                                penalty.createdAt,
                                Pageable.ofSize(1),
                            ).firstOrNull()
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
