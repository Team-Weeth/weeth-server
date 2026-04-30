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

    /**
     * applyOb에서 다음 기수로 등록하기 위해 사용하는 메서드
     * 하위호환을 위해 기수가 없는 경우라도 다음 기수 활동이 가능하도록 지원
     * TODO: 앞 단에서 기수가 필수로 저장됨을 보장해야함. (가입, 기수 추가 등)
     */
    fun isLatestOrFirstCardinal(
        clubMember: ClubMember,
        cardinal: Cardinal,
    ): Boolean {
        val latest = clubMemberCardinalReader.findLatestCardinalByClubMember(clubMember)
        return latest == null || cardinal.cardinalNumber > latest.cardinal.cardinalNumber
    }
}
