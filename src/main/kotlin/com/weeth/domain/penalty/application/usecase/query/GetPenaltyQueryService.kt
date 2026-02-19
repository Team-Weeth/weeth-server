package com.weeth.domain.penalty.application.usecase.query

import com.weeth.domain.penalty.application.dto.response.PenaltyByCardinalResponse
import com.weeth.domain.penalty.application.dto.response.PenaltyResponse
import com.weeth.domain.penalty.application.mapper.PenaltyMapper
import com.weeth.domain.penalty.domain.repository.PenaltyRepository
import com.weeth.domain.user.domain.service.CardinalGetService
import com.weeth.domain.user.domain.service.UserCardinalGetService
import com.weeth.domain.user.domain.service.UserGetService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetPenaltyQueryService(
    private val penaltyRepository: PenaltyRepository,
    private val userGetService: UserGetService,
    private val userCardinalGetService: UserCardinalGetService,
    private val cardinalGetService: CardinalGetService,
    private val mapper: PenaltyMapper,
) {
    fun findAllByCardinal(cardinalNumber: Int?): List<PenaltyByCardinalResponse> {
        val cardinals =
            if (cardinalNumber == null) {
                cardinalGetService.findAllCardinalNumberDesc()
            } else {
                listOf(cardinalGetService.findByAdminSide(cardinalNumber))
            }

        return cardinals.map { cardinal ->
            val penalties = penaltyRepository.findByCardinalIdOrderByIdDesc(cardinal.id)
            val users = penalties.map { it.user }.distinct()
            val userCardinalsMap =
                userCardinalGetService
                    .findAll(users)
                    .groupBy { it.user.id }

            val responses =
                penalties
                    .groupBy { it.user.id }
                    .entries
                    .map { (userId, userPenalties) ->
                        val userCardinals = userCardinalsMap[userId] ?: emptyList()
                        mapper.toResponse(userPenalties.first().user, userPenalties, userCardinals)
                    }.sortedBy { it.userId }

            mapper.toByCardinalResponse(cardinal.cardinalNumber, responses)
        }
    }

    fun findByUser(userId: Long): PenaltyResponse {
        val user = userGetService.find(userId)
        val currentCardinal = userCardinalGetService.getCurrentCardinal(user)
        val penalties = penaltyRepository.findByUserIdAndCardinalIdOrderByIdDesc(userId, currentCardinal.id)
        val userCardinals = userCardinalGetService.getUserCardinals(user)

        return mapper.toResponse(user, penalties, userCardinals)
    }
}
