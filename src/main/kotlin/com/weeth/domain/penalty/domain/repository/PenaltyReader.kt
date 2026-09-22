package com.weeth.domain.penalty.domain.repository

import com.weeth.domain.penalty.domain.entity.Penalty
import com.weeth.domain.penalty.domain.enums.PenaltyType
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice

interface PenaltyReader {
    fun countByClubMemberIdAndCardinalId(
        clubMemberId: Long,
        cardinalId: Long,
    ): Int

    /** cardinalId가 null이면 기수 필터 없이 멤버의 penaltyType별 건수를 센다. */
    fun countByClubMemberIdAndCardinalIdAndPenaltyType(
        clubMemberId: Long,
        cardinalId: Long?,
        penaltyType: PenaltyType,
    ): Int

    fun findByClubMemberIds(clubMemberIds: List<Long>): List<Penalty>

    /** cardinalId가 null이면 기수 필터 없이 멤버의 전체 페널티를 최신순으로 슬라이스 조회한다. */
    fun findSliceByClubMemberId(
        clubMemberId: Long,
        cardinalId: Long?,
        pageable: Pageable,
    ): Slice<Penalty>
}
