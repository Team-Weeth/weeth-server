package com.weeth.domain.club.application.usecase.query

import com.weeth.domain.club.application.dto.response.ClubMemberProfileResponse
import com.weeth.domain.club.application.dto.response.ClubMemberResponse
import com.weeth.domain.club.application.mapper.ClubMapper
import com.weeth.domain.club.domain.repository.ClubMemberCardinalReader
import com.weeth.domain.club.domain.repository.ClubMemberReader
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.club.domain.service.ClubPermissionPolicy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetClubMemberQueryService(
    private val clubMemberReader: ClubMemberReader,
    private val clubMemberCardinalReader: ClubMemberCardinalReader,
    private val clubMemberPolicy: ClubMemberPolicy,
    private val clubPermissionPolicy: ClubPermissionPolicy,
    private val clubMapper: ClubMapper,
) {
    fun findClubMembersForAdmin(
        clubId: Long,
        userId: Long,
    ): List<ClubMemberResponse> {
        clubPermissionPolicy.requireAdmin(clubId, userId)
        val members = clubMemberReader.findAllByClubId(clubId)

        if (members.isEmpty()) {
            return emptyList()
        }

        val allMemberCardinals = clubMemberCardinalReader.findAllByClubMembers(members)
        val memberCardinalMap = allMemberCardinals.groupBy { it.clubMember.id }

        return members.map { member ->
            val cardinals = memberCardinalMap[member.id] ?: emptyList()
            clubMapper.toMemberResponse(member, cardinals)
        }
    }

    fun findMyMemberProfile(
        clubId: Long,
        userId: Long,
    ): ClubMemberProfileResponse {
        val member = clubMemberPolicy.getActiveMember(clubId, userId)
        val cardinals = clubMemberCardinalReader.findAllByClubMember(member)

        return clubMapper.toMemberProfileResponse(member, cardinals)
    }
}
