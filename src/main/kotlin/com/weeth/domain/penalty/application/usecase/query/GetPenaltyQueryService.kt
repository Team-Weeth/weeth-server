package com.weeth.domain.penalty.application.usecase.query

import com.weeth.domain.penalty.application.dto.response.PenaltyByCardinalResponse
import com.weeth.domain.penalty.application.dto.response.PenaltyResponse
import com.weeth.domain.penalty.application.mapper.PenaltyMapper
import com.weeth.domain.penalty.domain.repository.PenaltyRepository
import com.weeth.domain.user.domain.repository.CardinalReader
import com.weeth.domain.user.domain.repository.UserCardinalReader
import com.weeth.domain.user.domain.repository.UserReader
import com.weeth.domain.user.domain.service.UserCardinalPolicy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetPenaltyQueryService(
    private val penaltyRepository: PenaltyRepository,
    private val userReader: UserReader,
    private val userCardinalReader: UserCardinalReader,
    private val userCardinalPolicy: UserCardinalPolicy,
    private val cardinalReader: CardinalReader,
    private val mapper: PenaltyMapper,
) {
    fun findAllByCardinal(cardinalNumber: Int?): List<PenaltyByCardinalResponse> {
        val cardinals =
            if (cardinalNumber == null) {
                cardinalReader.findAllByCardinalNumberDesc()
            } else {
                listOf(cardinalReader.getByCardinalNumber(cardinalNumber))
            }

        return cardinals.map { cardinal ->
            val penalties = penaltyRepository.findByCardinalIdOrderByIdDesc(cardinal.id)
            val users = penalties.map { it.user }.distinct()
            val userCardinalsMap =
                userCardinalReader
                    .findAllByUsersOrderByCardinalDesc(users)
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
        val user = userReader.getById(userId)
        val currentCardinal = userCardinalPolicy.getCurrentCardinal(user)
        val penalties = penaltyRepository.findByUserIdAndCardinalIdOrderByIdDesc(userId, currentCardinal.id)
        val userCardinals = userCardinalReader.findAllByUser(user)

        return mapper.toResponse(user, penalties, userCardinals)
    }
}
