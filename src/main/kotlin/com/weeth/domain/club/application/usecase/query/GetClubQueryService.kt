package com.weeth.domain.club.application.usecase.query

import com.weeth.domain.club.application.dto.response.ClubDetailResponse
import com.weeth.domain.club.application.dto.response.ClubInfoResponse
import com.weeth.domain.club.application.dto.response.ClubResponse
import com.weeth.domain.club.application.mapper.ClubMapper
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
        val members = clubMemberReader.findAllByUserId(userId)

        return members.map { member ->
            val club = clubReader.getClubById(member.club.id)
            clubMapper.toInfoResponse(club, member)
        }
    }

    fun findClub(clubId: Long): ClubResponse {
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
}
