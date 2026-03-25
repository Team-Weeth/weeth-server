package com.weeth.domain.club.application.mapper

import com.weeth.domain.club.application.dto.response.ClubDetailResponse
import com.weeth.domain.club.application.dto.response.ClubInfoResponse
import com.weeth.domain.club.application.dto.response.ClubMemberProfileResponse
import com.weeth.domain.club.application.dto.response.ClubMemberResponse
import com.weeth.domain.club.application.dto.response.ClubMemberSummaryResponse
import com.weeth.domain.club.application.dto.response.ClubMembershipStatusResponse
import com.weeth.domain.club.application.dto.response.ClubPublicResponse
import com.weeth.domain.club.domain.entity.Club
import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.club.domain.entity.ClubMemberCardinal
import com.weeth.domain.club.domain.enums.MemberStatus
import com.weeth.domain.file.domain.port.FileAccessUrlPort
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

        return ClubMembershipStatusResponse(
            hasActiveClub = activeMember != null,
            hasWaitingClub = waitingMember != null,
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

    private fun resolveClubImage(storageKey: String?): String? = storageKey?.let { fileAccessUrlPort.resolve(it) }

    private fun toCardinalNumbers(cardinals: List<ClubMemberCardinal>): List<Int> {
        if (cardinals.isEmpty()) {
            return emptyList()
        }

        return cardinals
            .map { it.cardinal.cardinalNumber }
            .sorted()
    }
}
