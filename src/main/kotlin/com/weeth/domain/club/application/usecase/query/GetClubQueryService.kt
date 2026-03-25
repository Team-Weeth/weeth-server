package com.weeth.domain.club.application.usecase.query

import com.weeth.domain.club.application.dto.response.ClubDetailResponse
import com.weeth.domain.club.application.dto.response.ClubInfoResponse
import com.weeth.domain.club.application.dto.response.ClubMembershipStatusResponse
import com.weeth.domain.club.application.dto.response.ClubPublicResponse
import com.weeth.domain.club.application.mapper.ClubMapper
import com.weeth.domain.club.domain.repository.ClubMemberCardinalReader
import com.weeth.domain.club.domain.repository.ClubMemberReader
import com.weeth.domain.club.domain.repository.ClubReader
import com.weeth.domain.club.domain.service.ClubPermissionPolicy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetClubQueryService(
    private val clubReader: ClubReader,
    private val clubMemberReader: ClubMemberReader,
    private val clubMemberCardinalReader: ClubMemberCardinalReader,
    private val clubPermissionPolicy: ClubPermissionPolicy,
    private val clubMapper: ClubMapper,
) {
    fun findMyClubs(userId: Long): List<ClubInfoResponse> {
        val members = clubMemberReader.findAllByUserIdWithClub(userId)
        if (members.isEmpty()) return emptyList()

        val cardinalsByMemberId =
            clubMemberCardinalReader
                .findAllByClubMembers(members)
                .groupBy { it.clubMember.id }

        return members.map { member ->
            val cardinals = cardinalsByMemberId[member.id] ?: emptyList()
            val memberCount = clubMemberReader.countActiveByClubId(member.club.id)
            clubMapper.toInfoResponse(member.club, member, cardinals, memberCount)
        }
    }

    fun findClub(clubId: Long): ClubPublicResponse {
        val club = clubReader.getClubById(clubId)

        return clubMapper.toResponse(club)
    }

    fun findClubDetailForAdmin(
        clubId: Long,
        userId: Long,
    ): ClubDetailResponse {
        clubPermissionPolicy.requireAdmin(clubId, userId)
        val club = clubReader.getClubById(clubId)

        return clubMapper.toDetailResponse(club)
    }

    fun findMembershipStatus(userId: Long): ClubMembershipStatusResponse {
        val members = clubMemberReader.findAllByUserIdWithClub(userId)
        if (members.isEmpty()) {
            return clubMapper.toMembershipStatusResponse(members, emptyMap(), emptyMap())
        }

        val cardinalsByMemberId =
            clubMemberCardinalReader
                .findAllByClubMembers(members)
                .groupBy { it.clubMember.id }

        val memberCountByClubId =
            members
                .map { it.club.id }
                .distinct()
                .associateWith { clubMemberReader.countActiveByClubId(it) }

        return clubMapper.toMembershipStatusResponse(members, cardinalsByMemberId, memberCountByClubId)
    }
}
