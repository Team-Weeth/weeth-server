package com.weeth.domain.club.domain.service

import com.weeth.domain.club.application.exception.ClubCreateLimitExceededException
import com.weeth.domain.club.application.exception.ClubJoinLimitExceededException
import com.weeth.domain.club.domain.enums.MemberRole
import com.weeth.domain.club.domain.enums.MemberStatus
import com.weeth.domain.club.domain.repository.ClubMemberReader
import org.springframework.stereotype.Service

/**
 * 동아리 가입/생성 수 제한 검증 정책
 */
@Service
class ClubJoinPolicy(
    private val clubMemberReader: ClubMemberReader,
) {
    /**
     * 일반 멤버(USER)로 가입 가능한 동아리 수 제한 검증
     */
    fun validateJoinLimit(userId: Long) {
        val activeUserCount =
            clubMemberReader.countByUserIdAndMemberStatusAndMemberRole(
                userId,
                MemberStatus.ACTIVE,
                MemberRole.USER,
            )
        if (activeUserCount >= MAX_USER_CLUBS) {
            throw ClubJoinLimitExceededException()
        }
    }

    /**
     * 동아리장(LEAD)으로 생성 가능한 동아리 수 제한 검증
     */
    fun validateCreateLimit(userId: Long) {
        val activeLeadCount =
            clubMemberReader.countByUserIdAndMemberStatusAndMemberRole(
                userId,
                MemberStatus.ACTIVE,
                MemberRole.LEAD,
            )
        if (activeLeadCount >= MAX_LEAD_CLUBS) {
            throw ClubCreateLimitExceededException()
        }
    }

    companion object {
        private const val MAX_LEAD_CLUBS = 1
        private const val MAX_USER_CLUBS = 1
    }
}
