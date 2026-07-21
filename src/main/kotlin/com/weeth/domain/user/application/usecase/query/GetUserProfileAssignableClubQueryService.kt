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
                .map(::toResponse)

        return UserProfileAssignableClubsResponse(clubs = clubs)
    }

    private fun toResponse(club: Club): UserProfileAssignableClubResponse =
        UserProfileAssignableClubResponse(
            clubId = TsidBase62Encoder.encode(club.id),
            name = club.name,
            clubImage = club.profileImageStorageKey?.let(fileAccessUrlPort::resolve),
            clubMemberNumber = clubMemberReader.countActiveByClubId(club.id),
        )
}
