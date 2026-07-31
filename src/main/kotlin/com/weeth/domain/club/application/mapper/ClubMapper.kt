package com.weeth.domain.club.application.mapper

import com.weeth.domain.club.application.dto.response.ClubCreateResponse
import com.weeth.domain.club.application.dto.response.ClubDetailResponse
import com.weeth.domain.club.application.dto.response.ClubInfoResponse
import com.weeth.domain.club.application.dto.response.ClubMemberProfileResponse
import com.weeth.domain.club.application.dto.response.ClubMemberResponse
import com.weeth.domain.club.application.dto.response.ClubMemberSummaryResponse
import com.weeth.domain.club.application.dto.response.ClubMembershipStatusResponse
import com.weeth.domain.club.application.dto.response.ClubPublicResponse
import com.weeth.domain.club.application.dto.response.ClubUsingProfileResponse
import com.weeth.domain.club.application.dto.response.ProfileStatusResponse
import com.weeth.domain.club.domain.entity.Club
import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.club.domain.entity.ClubMemberCardinal
import com.weeth.domain.club.domain.enums.MemberStatus
import com.weeth.domain.file.domain.port.FileAccessUrlPort
import com.weeth.domain.user.domain.entity.User
import com.weeth.global.common.id.TsidBase62Encoder
import org.springframework.stereotype.Component

@Component
class ClubMapper(
    private val fileAccessUrlPort: FileAccessUrlPort,
) {
    fun toInfoResponse(
        club: Club,
        member: ClubMember,
        cardinals: List<ClubMemberCardinal>,
        memberCount: Long,
    ) = ClubInfoResponse(
        id = TsidBase62Encoder.encode(club.id),
        name = club.name,
        schoolName = club.schoolName,
        description = club.description,
        profileImageUrl = resolveClubImage(club.profileImageStorageKey),
        memberCount = memberCount,
        cardinals = toCardinalNumbers(cardinals),
        memberRole = member.memberRole,
        memberStatus = member.memberStatus,
        usingProfile = toUsingProfileResponse(member),
    )

    fun toResponse(club: Club) =
        ClubPublicResponse(
            id = TsidBase62Encoder.encode(club.id),
            name = club.name,
            description = club.description,
            profileImageUrl = resolveClubImage(club.profileImageStorageKey),
        )

    fun toDetailResponse(club: Club) =
        ClubDetailResponse(
            id = TsidBase62Encoder.encode(club.id),
            name = club.name,
            code = club.code,
            schoolName = club.schoolName,
            description = club.description,
            contactEmail = club.clubContact.email,
            contactPhoneNumber = club.clubContact.phoneNumber,
            primaryContact = club.clubContact.primaryContact,
            profileImageUrl = resolveClubImage(club.profileImageStorageKey),
            backgroundImageUrl = resolveClubImage(club.backgroundImageStorageKey),
        )

    fun toMemberResponse(
        member: ClubMember,
        cardinals: List<ClubMemberCardinal>,
    ) = ClubMemberResponse(
        userId = member.user.id,
        clubMemberId = member.id,
        name = member.user.name,
        email = member.user.emailValue,
        tel = member.user.telValue,
        school = member.user.school,
        department = member.user.department,
        studentId = member.user.studentId,
        cardinals = toCardinalNumbers(cardinals),
        memberStatus = member.memberStatus,
        memberRole = member.memberRole,
        attendanceCount = member.attendanceStats.attendanceCount,
        absenceCount = member.attendanceStats.absenceCount,
        attendanceRate = member.attendanceStats.attendanceRate,
        penaltyCount = member.penaltyCount,
        profileImageUrl = resolveMemberProfileImage(member),
        bio = resolveMemberBio(member),
        joinedAt = member.createdAt,
    )

    fun toMemberProfileResponse(
        member: ClubMember,
        cardinals: List<ClubMemberCardinal>,
    ) = ClubMemberProfileResponse(
        userId = member.user.id,
        clubMemberId = member.id,
        name = member.user.name,
        email = member.user.emailValue,
        tel = member.user.telValue,
        school = member.user.school,
        department = member.user.department,
        studentId = member.user.studentId,
        cardinals = toCardinalNumbers(cardinals),
        memberRole = member.memberRole,
        memberStatus = member.memberStatus,
        profileImageUrl = member.profileImageStorageKey?.let { fileAccessUrlPort.resolve(it) },
        bio = member.bio,
    )

    fun toMemberSummaryResponse(
        member: ClubMember,
        cardinals: List<ClubMemberCardinal>,
    ) = ClubMemberSummaryResponse(
        userId = member.user.id,
        name = member.user.name,
        cardinals = toCardinalNumbers(cardinals),
        role = member.memberRole,
    )

    fun toMembershipStatusResponse(
        members: List<ClubMember>,
        cardinalsByMemberId: Map<Long, List<ClubMemberCardinal>>,
        memberCountByClubId: Map<Long, Long>,
    ): ClubMembershipStatusResponse {
        val activeMember = members.firstOrNull { it.memberStatus == MemberStatus.ACTIVE }
        val waitingMember = members.firstOrNull { it.memberStatus == MemberStatus.WAITING }
        val bannedMember = members.firstOrNull { it.memberStatus == MemberStatus.BANNED }

        return ClubMembershipStatusResponse(
            hasActiveClub = activeMember != null,
            hasWaitingClub = waitingMember != null,
//            hasBannedClub = bannedMember != null, 추후 추가
            activeClub =
                activeMember?.let {
                    toInfoResponse(
                        it.club,
                        it,
                        cardinalsByMemberId[it.id] ?: emptyList(),
                        memberCountByClubId[it.club.id] ?: 0,
                    )
                },
            waitingClub =
                waitingMember?.let {
                    toInfoResponse(
                        it.club,
                        it,
                        cardinalsByMemberId[it.id] ?: emptyList(),
                        memberCountByClubId[it.club.id] ?: 0,
                    )
                },
        )
    }

    fun toProfileStatusResponse(
        user: User,
        cardinalAssigned: Boolean,
    ) = ProfileStatusResponse(
        profileCompleted = user.isProfileCompleted(),
        cardinalAssigned = cardinalAssigned,
        missingFields = user.missingProfileFields(),
    )

    fun toCreateResponse(club: Club) =
        ClubCreateResponse(
            clubId = TsidBase62Encoder.encode(club.id),
            clubName = club.name,
        )

    private fun resolveClubImage(storageKey: String?): String? = storageKey?.let { fileAccessUrlPort.resolve(it) }

    // 멀티프로필 도입 이후 멤버가 동아리에서 노출하는 프로필은 userProfile이다.
    // ClubMember의 동명 필드는 멀티프로필 이전 데이터라 fallback으로만 사용한다.
    private fun resolveMemberProfileImage(member: ClubMember): String? =
        (member.userProfile?.profileImageStorageKey ?: member.profileImageStorageKey)
            ?.let { fileAccessUrlPort.resolve(it) }

    private fun resolveMemberBio(member: ClubMember): String? = member.userProfile?.bio ?: member.bio

    private fun toUsingProfileResponse(member: ClubMember): ClubUsingProfileResponse? =
        member.userProfile?.let { profile ->
            ClubUsingProfileResponse(
                profileId = profile.id,
                name = profile.name,
                profileImageUrl = profile.profileImageStorageKey?.let { fileAccessUrlPort.resolve(it) },
                bio = profile.bio,
            )
        }

    private fun toCardinalNumbers(cardinals: List<ClubMemberCardinal>): List<Int> {
        if (cardinals.isEmpty()) {
            return emptyList()
        }

        return cardinals
            .map { it.cardinal.cardinalNumber }
            .sorted()
    }
}
