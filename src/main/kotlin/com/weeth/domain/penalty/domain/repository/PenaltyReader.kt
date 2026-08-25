package com.weeth.domain.penalty.domain.repository

import com.weeth.domain.penalty.domain.entity.Penalty
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice

interface PenaltyReader {
    fun countByClubMemberIdAndCardinalId(
        clubMemberId: Long,
        cardinalId: Long,
    ): Int

    fun findByClubMemberIds(clubMemberIds: List<Long>): List<Penalty>

    fun findSliceByClubMemberId(
        clubMemberId: Long,
        pageable: Pageable,
    ): Slice<Penalty>
}
