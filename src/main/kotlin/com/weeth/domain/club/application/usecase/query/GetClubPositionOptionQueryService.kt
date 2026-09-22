package com.weeth.domain.club.application.usecase.query

import com.weeth.domain.club.application.dto.response.ClubPositionOptionResponse
import com.weeth.domain.club.application.mapper.ClubPositionOptionMapper
import com.weeth.domain.club.domain.repository.ClubPositionOptionReader
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.club.domain.service.ClubPermissionPolicy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetClubPositionOptionQueryService(
    private val clubPositionOptionReader: ClubPositionOptionReader,
    private val clubPermissionPolicy: ClubPermissionPolicy,
    private val clubMemberPolicy: ClubMemberPolicy,
    private val clubPositionOptionMapper: ClubPositionOptionMapper,
) {
    fun findAll(
        clubId: Long,
        userId: Long,
    ): List<ClubPositionOptionResponse> {
        clubPermissionPolicy.requireAdmin(clubId, userId)

        val options = clubPositionOptionReader.findAllByClubIdOrderByDisplayOrderAsc(clubId)
        return clubPositionOptionMapper.toResponses(options)
    }

    fun findAllForMember(
        clubId: Long,
        userId: Long,
    ): List<ClubPositionOptionResponse> {
        clubMemberPolicy.getActiveMember(clubId, userId)

        val options = clubPositionOptionReader.findAllByClubIdOrderByDisplayOrderAsc(clubId)
        return clubPositionOptionMapper.toResponses(options)
    }
}
