package com.weeth.domain.penalty.application.usecase.query

import com.weeth.domain.penalty.application.dto.response.PenaltyByCardinalResponse
import com.weeth.domain.penalty.application.dto.response.PenaltyResponse
import com.weeth.domain.penalty.application.mapper.PenaltyMapper
import com.weeth.domain.penalty.domain.entity.Penalty
import com.weeth.domain.penalty.domain.repository.PenaltyRepository
import com.weeth.domain.user.domain.entity.User
import com.weeth.domain.user.domain.service.CardinalGetService
import com.weeth.domain.user.domain.service.UserCardinalGetService
import com.weeth.domain.user.domain.service.UserGetService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetPenaltyQueryService(
    private val penaltyRepository: PenaltyRepository,
    private val userGetService: UserGetService,
    private val userCardinalGetService: UserCardinalGetService,
    private val cardinalGetService: CardinalGetService,
    private val mapper: PenaltyMapper,
) {
    @Transactional(readOnly = true)
    fun findAll(cardinalNumber: Int?): List<PenaltyByCardinalResponse> {
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
                    .map { (_, userPenalties) -> toPenaltyResponse(userPenalties.first().user, userPenalties) }
                    .sortedBy { it.userId }

            mapper.toByCardinalResponse(cardinal.cardinalNumber, responses)
        }
    }

    @Transactional(readOnly = true)
    fun find(userId: Long): PenaltyResponse {
        val user = userGetService.find(userId)
        val currentCardinal = userCardinalGetService.getCurrentCardinal(user)
        val penalties = penaltyRepository.findByUserIdAndCardinalIdOrderByIdDesc(userId, currentCardinal.id)

        return toPenaltyResponse(user, penalties)
    }

    private fun toPenaltyResponse(
        user: User,
        penalties: List<Penalty>,
    ): PenaltyResponse {
        val userCardinals = userCardinalGetService.getUserCardinals(user)
        val penaltyDetails = penalties.map(mapper::toDetailResponse)
        return mapper.toResponse(user, penaltyDetails, userCardinals)
    }
}
