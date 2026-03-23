package com.weeth.domain.club.domain.service

import com.weeth.domain.club.application.exception.NotClubAdminException
import com.weeth.domain.club.domain.entity.ClubMember
import org.springframework.stereotype.Service

/**
 * 동아리 관리자 권한 검증 정책
 */
@Service
class ClubPermissionPolicy(
    private val clubMemberPolicy: ClubMemberPolicy,
) {
    /**
     * 사용자가 동아리 관리자인지 검증
     * 활성 상태이고 + ADMIN 또는 LEAD 권한
     */
    fun requireAdmin(
        clubId: Long,
        userId: Long,
    ): ClubMember =
        clubMemberPolicy.getActiveMember(clubId, userId).also {
            if (!it.isAdminOrLead()) {
                throw NotClubAdminException()
            }
        }
}
