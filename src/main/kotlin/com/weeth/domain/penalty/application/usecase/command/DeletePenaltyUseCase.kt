package com.weeth.domain.penalty.application.usecase.command

import com.weeth.domain.penalty.application.exception.AutoPenaltyDeleteNotAllowedException
import com.weeth.domain.penalty.application.exception.PenaltyNotFoundException
import com.weeth.domain.penalty.domain.enums.PenaltyType
import com.weeth.domain.penalty.domain.repository.PenaltyRepository
import com.weeth.domain.user.application.exception.UserNotFoundException
import com.weeth.domain.user.domain.repository.UserRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

//todo: PR4에서 Club 기반으로 수정
@Service
class DeletePenaltyUseCase(
    private val penaltyRepository: PenaltyRepository,
    private val userRepository: UserRepository, // 타 도메인이므로 Reader 사용 검토 (조회 시에는 Reader, 업데이트 시에는 Repository)
) {
    /**
     * Todo: 코드 가독성 개선 및 트랜잭션 범위 축소
     */
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

            else -> {}
        }

        penaltyRepository.deleteById(penaltyId)
    }
}
