package com.weeth.domain.club.application.usecase.query

import com.weeth.domain.club.application.dto.response.ClubDetailResponse
import com.weeth.domain.club.application.dto.response.ClubInfoResponse
import com.weeth.domain.club.application.dto.response.ClubMembershipStatusResponse
import com.weeth.domain.club.application.dto.response.ClubPublicResponse
import com.weeth.domain.club.application.mapper.ClubMapper
import com.weeth.domain.club.domain.enums.MemberStatus
import com.weeth.domain.club.domain.repository.ClubMemberReader
import com.weeth.domain.club.domain.repository.ClubReader
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetClubQueryService(
    private val clubReader: ClubReader,
    private val clubMemberReader: ClubMemberReader,
    private val clubMemberPolicy: ClubMemberPolicy,
    private val clubMapper: ClubMapper,
) {
    fun findMyClubs(userId: Long): List<ClubInfoResponse> {
        val members = clubMemberReader.findAllByUserIdWithClub(userId)

        return members.map { member ->
            clubMapper.toInfoResponse(member.club, member)
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
        clubMemberPolicy.requireAdmin(clubId, userId)
        val club = clubReader.getClubById(clubId)

        return clubMapper.toDetailResponse(club)
    }

    fun findMembershipStatus(userId: Long): ClubMembershipStatusResponse {
        val members = clubMemberReader.findAllByUserIdWithClub(userId)

        val activeMember = members.firstOrNull { it.memberStatus == MemberStatus.ACTIVE }
        val waitingMember = members.firstOrNull { it.memberStatus == MemberStatus.WAITING }

        val activeClub = activeMember?.let { clubMapper.toInfoResponse(it.club, it) }
        val waitingClub = waitingMember?.let { clubMapper.toInfoResponse(it.club, it) }

        return ClubMembershipStatusResponse(
            hasActiveClub = activeMember != null,
            hasWaitingClub = waitingMember != null,
            activeClub = activeClub,
            waitingClub = waitingClub,
        )
    }
}
