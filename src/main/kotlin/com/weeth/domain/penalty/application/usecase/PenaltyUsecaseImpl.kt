package com.weeth.domain.penalty.application.usecase

import com.weeth.domain.penalty.application.dto.PenaltyDTO
import com.weeth.domain.penalty.application.exception.AutoPenaltyDeleteNotAllowedException
import com.weeth.domain.penalty.application.exception.PenaltyNotFoundException
import com.weeth.domain.penalty.application.mapper.PenaltyMapper
import com.weeth.domain.penalty.domain.entity.Penalty
import com.weeth.domain.penalty.domain.entity.enums.PenaltyType
import com.weeth.domain.penalty.domain.repository.PenaltyRepository
import com.weeth.domain.user.domain.service.CardinalGetService
import com.weeth.domain.user.domain.service.UserCardinalGetService
import com.weeth.domain.user.domain.service.UserGetService
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PenaltyUsecaseImpl(
    private val penaltyRepository: PenaltyRepository,
    private val userGetService: UserGetService,
    private val userCardinalGetService: UserCardinalGetService,
    private val cardinalGetService: CardinalGetService,
    private val mapper: PenaltyMapper,
) : PenaltyUsecase {
    companion object {
        private const val AUTO_PENALTY_DESCRIPTION = "누적경고 %d회"
    }

    @Transactional
    override fun save(dto: PenaltyDTO.Save) {
        val user = userGetService.find(dto.userId)
        val cardinal = userCardinalGetService.getCurrentCardinal(user)

        val penalty = mapper.fromPenaltyDto(dto, user, cardinal)
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
                    val autoPenalty = mapper.toAutoPenalty(description, user, cardinal, PenaltyType.AUTO_PENALTY)
                    penaltyRepository.save(autoPenalty)
                    user.incrementPenaltyCount()
                }
            }

            else -> {}
        }
    }

    @Transactional
    override fun update(dto: PenaltyDTO.Update) {
        val penalty =
            penaltyRepository.findByIdOrNull(dto.penaltyId)
                ?: throw PenaltyNotFoundException()

        if (!dto.penaltyDescription.isNullOrBlank()) {
            penalty.update(dto.penaltyDescription)
        }
    }

    // TODO: 쿼리 최적화 필요
    @Transactional(readOnly = true)
    override fun findAll(cardinalNumber: Int?): List<PenaltyDTO.ResponseAll> {
        val cardinals =
            if (cardinalNumber == null) {
                cardinalGetService.findAllCardinalNumberDesc()
            } else {
                listOf(cardinalGetService.findByAdminSide(cardinalNumber))
            }

        return cardinals.map { cardinal ->
            val penalties = penaltyRepository.findByCardinalIdOrderByIdDesc(cardinal.id)

            val responses =
                penalties
                    .groupBy { it.user.id }
                    .entries
                    .map { (userId, userPenalties) -> toPenaltyDto(userId, userPenalties) }
                    .sortedBy { it.userId }

            mapper.toResponseAll(cardinal.cardinalNumber, responses)
        }
    }

    @Transactional(readOnly = true)
    override fun find(userId: Long): PenaltyDTO.Response {
        val user = userGetService.find(userId)
        val currentCardinal = userCardinalGetService.getCurrentCardinal(user)
        val penalties = penaltyRepository.findByUserIdAndCardinalIdOrderByIdDesc(userId, currentCardinal.id)

        return toPenaltyDto(userId, penalties)
    }

    @Transactional
    override fun delete(penaltyId: Long) {
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

    private fun toPenaltyDto(
        userId: Long,
        penalties: List<Penalty>,
    ): PenaltyDTO.Response {
        val user = userGetService.find(userId)
        val userCardinals = userCardinalGetService.getUserCardinals(user)
        val penaltyDTOs = penalties.map(mapper::toPenalties)
        return mapper.toPenaltyDto(user, penaltyDTOs, userCardinals)
    }
}
