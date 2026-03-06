package com.weeth.domain.club.application.dto.response

import com.weeth.domain.club.domain.enums.MemberRole
import com.weeth.domain.club.domain.enums.MemberStatus
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 내 멤버 정보 조회 API에 사용
 */
data class ClubMemberProfileResponse(
    @field:Schema(description = "멤버 ID", example = "1")
    val clubMemberId: Long,
    @field:Schema(description = "멤버 상태", example = "ACTIVE")
    val memberStatus: MemberStatus,
    @field:Schema(description = "멤버 권한", example = "USER")
    val memberRole: MemberRole,
    @field:Schema(description = "출석 횟수", example = "10")
    val attendanceCount: Int,
    @field:Schema(description = "결석 횟수", example = "2")
    val absenceCount: Int,
    @field:Schema(description = "출석률 (%)", example = "83")
    val attendanceRate: Int,
    @field:Schema(description = "패널티 횟수", example = "1")
    val penaltyCount: Int,
)
