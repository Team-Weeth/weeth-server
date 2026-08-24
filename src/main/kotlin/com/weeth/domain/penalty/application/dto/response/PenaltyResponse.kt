package com.weeth.domain.penalty.application.dto.response

import com.weeth.domain.club.domain.enums.MemberStatus
import io.swagger.v3.oas.annotations.media.Schema

data class PenaltyResponse(
    @field:Schema(description = "사용자 ID", example = "1")
    val userId: Long,
    @field:Schema(description = "사용자 이름", example = "홍길동")
    val name: String,
    @field:Schema(description = "동아리 멤버 상태", example = "ACTIVE")
    val memberStatus: MemberStatus,
    @field:Schema(description = "페널티 횟수", example = "2")
    val penaltyCount: Int,
    @field:Schema(description = "소속 기수 목록", example = "[3, 4]")
    val cardinals: List<Int>,
    @field:Schema(description = "페널티 상세 목록")
    val penalties: List<PenaltyDetailResponse>,
)
