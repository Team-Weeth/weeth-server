package com.weeth.domain.user.application.usecase.query

import com.weeth.domain.club.domain.entity.Club
import com.weeth.domain.club.domain.enums.MemberStatus
import com.weeth.domain.club.domain.repository.ClubMemberReader
import com.weeth.domain.file.domain.port.FileAccessUrlPort
import com.weeth.domain.user.application.dto.response.UserProfileAssignableClubResponse
import com.weeth.domain.user.application.dto.response.UserProfileAssignableClubsResponse
import com.weeth.global.common.id.TsidBase62Encoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetUserProfileAssignableClubQueryService(
    private val clubMemberReader: ClubMemberReader,
    private val fileAccessUrlPort: FileAccessUrlPort,
) {
    @Transactional(readOnly = true)
    fun findAll(userId: Long): UserProfileAssignableClubsResponse {
        val clubs =
            clubMemberReader
                .findAllByUserIdAndMemberStatusWithClub(userId, MemberStatus.ACTIVE)
                .map { it.club }
                .sortedBy { it.id }
        if (clubs.isEmpty()) {
            return UserProfileAssignableClubsResponse(clubs = emptyList())
        }

        val memberCountByClubId =
            clubMemberReader
                .countActiveByClubIds(clubs.map { it.id })
                .associate { it.clubId to it.memberCount }

        return UserProfileAssignableClubsResponse(
            clubs = clubs.map { toResponse(it, memberCountByClubId[it.id] ?: 0L) },
        )
    }

    private fun toResponse(
        club: Club,
        memberCount: Long,
    ): UserProfileAssignableClubResponse =
        UserProfileAssignableClubResponse(
            clubId = TsidBase62Encoder.encode(club.id),
            name = club.name,
            clubImage = club.profileImageStorageKey?.let(fileAccessUrlPort::resolve),
            clubMemberNumber = memberCount,
        )
}
