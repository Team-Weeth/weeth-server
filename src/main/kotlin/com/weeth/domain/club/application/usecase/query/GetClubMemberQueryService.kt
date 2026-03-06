package com.weeth.domain.club.application.usecase.query

import com.weeth.domain.club.application.dto.response.ClubMemberProfileResponse
import com.weeth.domain.club.application.dto.response.ClubMemberResponse
import com.weeth.domain.club.application.mapper.ClubMapper
import com.weeth.domain.club.domain.repository.ClubMemberReader
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetClubMemberQueryService(
    private val clubMemberReader: ClubMemberReader,
    private val clubMemberPolicy: ClubMemberPolicy,
    private val clubMapper: ClubMapper,
) {
    fun findClubMembersForAdmin(
        clubId: Long,
        userId: Long,
    ): List<ClubMemberResponse> {
        clubMemberPolicy.requireAdmin(clubId, userId)
        val members = clubMemberReader.findAllByClubId(clubId)

        return members.map { clubMapper.toMemberResponse(it) }
    }

    fun findMyMemberProfile(
        clubId: Long,
        userId: Long,
    ): ClubMemberProfileResponse {
        val member = clubMemberPolicy.getActiveMember(clubId, userId)

        return clubMapper.toMemberProfileResponse(member)
    }
}
