package com.weeth.domain.penalty.application.usecase.command

import com.weeth.domain.penalty.application.dto.request.UpdatePenaltyRequest
import com.weeth.domain.penalty.application.exception.PenaltyNotFoundException
import com.weeth.domain.penalty.domain.repository.PenaltyRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UpdatePenaltyUseCase(
    private val penaltyRepository: PenaltyRepository,
) {
    @Transactional
    fun execute(request: UpdatePenaltyRequest) {
        val penalty =
            penaltyRepository.findByIdOrNull(request.penaltyId)
                ?: throw PenaltyNotFoundException()

        if (!request.penaltyDescription.isNullOrBlank()) {
            penalty.update(request.penaltyDescription)
        }
    }
}
