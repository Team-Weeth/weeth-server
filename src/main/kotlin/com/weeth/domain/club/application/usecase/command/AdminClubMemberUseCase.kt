package com.weeth.domain.club.application.usecase.command

import com.weeth.domain.club.application.dto.request.ClubMemberRoleUpdateRequest
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 동아리 관리자 전용 멤버 관리 UseCase
 */
@Service
class AdminClubMemberUseCase(
    private val clubMemberPolicy: ClubMemberPolicy,
) {
    @Transactional
    fun accept(
        clubId: Long,
        userId: Long,
        clubMemberId: Long,
    ) {
        clubMemberPolicy.requireAdmin(clubId, userId)

        val member = clubMemberPolicy.getMemberInClub(clubId, clubMemberId)
        member.accept()
    }

    @Transactional
    fun ban(
        clubId: Long,
        userId: Long,
        clubMemberId: Long,
    ) {
        clubMemberPolicy.requireAdmin(clubId, userId)

        val member = clubMemberPolicy.getMemberInClub(clubId, clubMemberId)
        member.ban()
    }

    @Transactional
    fun updateMemberRole(
        clubId: Long,
        userId: Long,
        request: ClubMemberRoleUpdateRequest,
    ) {
        clubMemberPolicy.requireAdmin(clubId, userId)

        val member = clubMemberPolicy.getMemberInClub(clubId, request.clubMemberId)
        member.updateRole(request.memberRole)
    }
}
