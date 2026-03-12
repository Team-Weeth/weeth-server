package com.weeth.domain.club.domain.service

import com.weeth.domain.cardinal.application.exception.CardinalNotFoundException
import com.weeth.domain.cardinal.domain.entity.Cardinal
import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.club.domain.repository.ClubMemberCardinalReader
import org.springframework.stereotype.Service

@Service
class ClubMemberCardinalPolicy(
    private val clubMemberCardinalReader: ClubMemberCardinalReader,
) {
    fun getCurrentCardinal(clubMember: ClubMember): Cardinal {
        val latest =
            clubMemberCardinalReader.findLatestCardinalByClubMember(clubMember)
                ?: throw CardinalNotFoundException()
        return latest.cardinal
    }

    fun notContains(
        clubMember: ClubMember,
        cardinal: Cardinal,
    ): Boolean = !clubMemberCardinalReader.existsByClubMemberAndCardinalId(clubMember, cardinal.id)

    fun isCurrent(
        clubMember: ClubMember,
        cardinal: Cardinal,
    ): Boolean {
        val latest = clubMemberCardinalReader.findLatestCardinalByClubMember(clubMember)
        return latest == null || cardinal.cardinalNumber > latest.cardinal.cardinalNumber
    }
}
