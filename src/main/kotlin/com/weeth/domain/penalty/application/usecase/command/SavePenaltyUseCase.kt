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
    private val userRepository: UserRepository, // 타 도메인이므로 Reader 사용 검토
    private val userCardinalPolicy: UserCardinalPolicy,
    private val mapper: PenaltyMapper,
) {
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

            else -> {} // BONUS 등 다른 유형은 카운트 변경 없음
        }
    }
}
