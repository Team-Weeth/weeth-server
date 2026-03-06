package com.weeth.domain.club.domain.repository

import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.club.domain.entity.ClubMemberCardinal

interface ClubMemberCardinalReader {
    fun findAllByClubMember(clubMember: ClubMember): List<ClubMemberCardinal>

    fun findAllByClubMembers(clubMembers: List<ClubMember>): List<ClubMemberCardinal>

    fun findLatestCardinalByClubMember(clubMember: ClubMember): ClubMemberCardinal?

    fun existsByClubMemberAndCardinalId( // todo: 실제 사용처에 따라 파라미터 확정
        clubMember: ClubMember,
        cardinalId: Long,
    ): Boolean
}
