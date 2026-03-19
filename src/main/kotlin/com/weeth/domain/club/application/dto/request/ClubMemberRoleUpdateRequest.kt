package com.weeth.domain.club.application.dto.request

import com.weeth.domain.club.domain.enums.MemberRole
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Positive

data class ClubMemberRoleUpdateRequest(
    @field:Schema(description = "멤버 ID", example = "1")
    @field:Positive
    val clubMemberId: Long,
    @field:Schema(description = "변경할 권한 (LEAD는 별도 API로 요청해주세요. 또한 LEAD는 사용자 뷰에 보이지 않게 해주세요)", example = "ADMIN")
    val memberRole: MemberRole,
)
