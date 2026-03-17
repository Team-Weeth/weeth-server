package com.weeth.domain.club.application.mapper

import com.weeth.domain.club.application.dto.response.ClubDetailResponse
import com.weeth.domain.club.application.dto.response.ClubInfoResponse
import com.weeth.domain.club.application.dto.response.ClubMemberProfileResponse
import com.weeth.domain.club.application.dto.response.ClubMemberResponse
import com.weeth.domain.club.application.dto.response.ClubPublicResponse
import com.weeth.domain.club.domain.entity.Club
import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.club.domain.entity.ClubMemberCardinal
import com.weeth.global.common.id.TsidBase62Encoder
import org.springframework.stereotype.Component

@Component
class ClubMapper {
    fun toInfoResponse(
        club: Club,
        member: ClubMember,
    ) = ClubInfoResponse(
        id = TsidBase62Encoder.encode(club.id),
        name = club.name,
        schoolName = club.schoolName,
        description = club.description,
        memberRole = member.memberRole,
        memberStatus = member.memberStatus,
    )

    fun toResponse(club: Club) =
        ClubPublicResponse(
            id = TsidBase62Encoder.encode(club.id),
            name = club.name,
            description = club.description,
            profileImageUrl = club.profileImageUrl,
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
            profileImageUrl = club.profileImageUrl,
            backgroundImageUrl = club.backgroundImageUrl,
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
        school = null, // todo: User 도메인 반영 작업시 학교 정보 추가
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
        school = null, // todo: User 도메인 반영 작업시 학교 정보 추가
        department = member.user.department,
        studentId = member.user.studentId,
        cardinals = toCardinalNumbers(cardinals),
    )

    private fun toCardinalNumbers(cardinals: List<ClubMemberCardinal>): List<Int> {
        if (cardinals.isEmpty()) {
            return emptyList()
        }

        return cardinals
            .map { it.cardinal.cardinalNumber }
            .sorted()
    }
}
