package com.weeth.domain.penalty.application.usecase.command

import com.weeth.domain.penalty.application.dto.request.SavePenaltyRequest
import com.weeth.domain.penalty.application.mapper.PenaltyMapper
import com.weeth.domain.penalty.domain.enums.PenaltyType
import com.weeth.domain.penalty.domain.repository.PenaltyRepository
import com.weeth.domain.user.application.exception.UserNotFoundException
import com.weeth.domain.user.domain.repository.UserRepository
import com.weeth.domain.user.domain.service.UserCardinalPolicy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SavePenaltyUseCase(
    private val penaltyRepository: PenaltyRepository,
    private val userRepository: UserRepository,
    private val userCardinalPolicy: UserCardinalPolicy,
    private val mapper: PenaltyMapper,
) {
    companion object {
        private const val AUTO_PENALTY_DESCRIPTION = "누적경고 %d회"
    }

    @Transactional
    fun save(request: SavePenaltyRequest) {
        val user =
            userRepository
                .findByIdWithLock(request.userId)
                .orElseThrow { UserNotFoundException() }
        val cardinal = userCardinalPolicy.getCurrentCardinal(user)

        val penalty = mapper.toEntity(request, user, cardinal)
        penaltyRepository.save(penalty)

        when (penalty.penaltyType) {
            PenaltyType.PENALTY -> {
                user.incrementPenaltyCount()
            }

            PenaltyType.WARNING -> {
                user.incrementWarningCount()

                val warningCount = user.warningCount
                if (warningCount % 2 == 0) {
                    val description = AUTO_PENALTY_DESCRIPTION.format(warningCount)
                    val autoPenalty = mapper.toAutoPenalty(description, user, cardinal)
                    penaltyRepository.save(autoPenalty)
                    user.incrementPenaltyCount()
                }
            }

            else -> {}
        }
    }
}
