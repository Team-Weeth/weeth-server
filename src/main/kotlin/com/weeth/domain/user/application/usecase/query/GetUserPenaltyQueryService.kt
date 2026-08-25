package com.weeth.domain.user.application.usecase.query

import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.penalty.domain.repository.PenaltyReader
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
) {
    fun getMyPenalties(
        userId: Long,
        clubId: Long,
        pageNumber: Int,
        pageSize: Int,
    ): SliceResponse<UserMyPenaltyResponse> {
        if (pageNumber < 0 || pageSize !in 1..MAX_PAGE_SIZE) throw UserPageNotFoundException()

        val clubMember = clubMemberPolicy.getActiveMember(clubId, userId)
        val pageable = PageRequest.of(pageNumber, pageSize)
        val penalties = penaltyReader.findSliceByClubMemberId(clubMember.id, pageable)

        return SliceResponse.from(
            penalties.map { penalty ->
                UserMyPenaltyResponse(
                    penaltyId = penalty.id,
                    score = penalty.score,
                    penaltyDescription = penalty.penaltyDescription,
                    penaltyType = penalty.penaltyType,
                    createdAt = penalty.createdAt,
                )
            },
        )
    }

    companion object {
        private const val MAX_PAGE_SIZE = 50
    }
}
