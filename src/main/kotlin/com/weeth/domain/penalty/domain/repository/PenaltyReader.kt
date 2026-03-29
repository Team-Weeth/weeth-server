package com.weeth.domain.penalty.domain.repository

interface PenaltyReader {
    fun countByClubMemberIdAndCardinalId(
        clubMemberId: Long,
        cardinalId: Long,
    ): Int
}
