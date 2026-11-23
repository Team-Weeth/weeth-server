package com.weeth.domain.notification.fixture

import com.weeth.domain.club.domain.entity.Club
import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.club.domain.enums.MemberRole
import com.weeth.domain.club.domain.enums.MemberStatus
import com.weeth.domain.club.domain.repository.ClubMemberRepository
import com.weeth.domain.user.domain.repository.UserRepository

object NotificationIntegrationTestFixture {
    fun ClubMemberRepository.saveMember(
        club: Club,
        users: UserRepository,
        role: MemberRole = MemberRole.USER,
        status: MemberStatus = MemberStatus.ACTIVE,
    ): ClubMember {
        val user = users.save(NotificationPreferenceTestFixture.createUser())
        return save(ClubMember(club = club, user = user, memberRole = role, memberStatus = status))
    }
}
