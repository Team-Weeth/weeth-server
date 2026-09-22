package com.weeth.domain.user.application.usecase.query

import com.weeth.domain.cardinal.domain.repository.CardinalReader
import com.weeth.domain.club.domain.repository.ClubMemberCardinalReader
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.penalty.domain.enums.PenaltyType
import com.weeth.domain.penalty.domain.repository.PenaltyReader
import com.weeth.domain.user.application.dto.response.UserMyPenaltyListResponse
import com.weeth.domain.user.application.dto.response.UserMyPenaltyResponse
import com.weeth.domain.user.application.exception.UserPageNotFoundException
import com.weeth.global.common.response.SliceResponse
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetUserPenaltyQueryService(
    private val penaltyReader: PenaltyReader,
    private val clubMemberPolicy: ClubMemberPolicy,
    private val clubMemberCardinalReader: ClubMemberCardinalReader,
    private val cardinalReader: CardinalReader,
) {
    fun getMyPenalties(
        userId: Long,
        clubId: Long,
        cardinalNumber: Int?,
        pageNumber: Int,
        pageSize: Int,
    ): UserMyPenaltyListResponse {
        if (pageNumber < 0 || pageSize !in 1..MAX_PAGE_SIZE) throw UserPageNotFoundException()

        val clubMember = clubMemberPolicy.getActiveMember(clubId, userId)
        val cardinals =
            clubMemberCardinalReader
                .findAllByClubMember(clubMember)
                .map { it.cardinal.cardinalNumber }
                .sorted()

        // 존재하지 않는 기수를 지정한 경우, 예외 대신 빈 결과를 반환한다 (GetPenaltyQueryService.findAllByCardinal와 동일한 관례).
        val cardinalId =
            cardinalNumber?.let {
                cardinalReader.findByClubIdAndCardinalNumber(clubId, it)?.id
                    ?: return UserMyPenaltyListResponse(
                        penaltyCount = 0,
                        warningCount = if (clubMember.club.warningEnabled) 0 else null,
                        cardinals = cardinals,
                        penalties =
                            SliceResponse(
                                content = emptyList(),
                                pageNumber = pageNumber,
                                pageSize = pageSize,
                                numberOfElements = 0,
                                hasNext = false,
                            ),
                    )
            }
        val pageable = PageRequest.of(pageNumber, pageSize)
        val penalties = penaltyReader.findSliceByClubMemberId(clubMember.id, cardinalId, pageable)
        val penaltyCount =
            penaltyReader.countByClubMemberIdAndCardinalIdAndPenaltyType(clubMember.id, cardinalId, PenaltyType.PENALTY)
        val warningCount =
            if (clubMember.club.warningEnabled) {
                penaltyReader.countByClubMemberIdAndCardinalIdAndPenaltyType(
                    clubMember.id,
                    cardinalId,
                    PenaltyType.WARNING,
                )
            } else {
                null
            }

        return UserMyPenaltyListResponse(
            penaltyCount = penaltyCount,
            warningCount = warningCount,
            cardinals = cardinals,
            penalties =
                SliceResponse.from(
                    penalties.map { penalty ->
                        UserMyPenaltyResponse(
                            penaltyId = penalty.id,
                            penaltyDescription = penalty.penaltyDescription,
                            penaltyType = penalty.penaltyType,
                            createdAt = penalty.createdAt,
                        )
                    },
                ),
        )
    }

    companion object {
        private const val MAX_PAGE_SIZE = 50
    }
}
