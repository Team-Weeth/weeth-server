package com.weeth.domain.penalty.application.usecase.command

import com.weeth.domain.penalty.application.dto.request.SavePenaltyRequest
import com.weeth.domain.penalty.application.mapper.PenaltyMapper
import com.weeth.domain.penalty.domain.enums.PenaltyType
import com.weeth.domain.penalty.domain.repository.PenaltyRepository
import com.weeth.domain.user.domain.repository.UserRepository
import com.weeth.domain.user.domain.service.UserCardinalGetService
import com.weeth.domain.user.domain.service.UserGetService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SavePenaltyUseCase(
    private val penaltyRepository: PenaltyRepository,
    private val userRepository: UserRepository,
    private val userGetService: UserGetService,
    private val userCardinalGetService: UserCardinalGetService,
    private val mapper: PenaltyMapper,
) {
    companion object {
        private const val AUTO_PENALTY_DESCRIPTION = "누적경고 %d회"
    }

    @Transactional
    fun execute(request: SavePenaltyRequest) {
        val user = userGetService.find(request.userId)
        val cardinal = userCardinalGetService.getCurrentCardinal(user)

        val penalty = mapper.toEntity(request, user, cardinal)
        penaltyRepository.save(penalty)

        // 카운트 수정 직전에만 비관적 락 획득
        userRepository.findByIdWithLock(request.userId)

        when (penalty.penaltyType) {
            PenaltyType.PENALTY -> {
                user.incrementPenaltyCount()
            }

            PenaltyType.WARNING -> {
                user.incrementWarningCount()

                val warningCount = user.warningCount
                if (warningCount % 2 == 0) {
                    val description = AUTO_PENALTY_DESCRIPTION.format(warningCount)
                    val autoPenalty = mapper.toAutoPenalty(description, user, cardinal, PenaltyType.AUTO_PENALTY)
                    penaltyRepository.save(autoPenalty)
                    user.incrementPenaltyCount()
                }
            }

            else -> {}
        }
    }
}
