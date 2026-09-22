package com.weeth.domain.user.application.dto.response

import com.weeth.global.common.response.SliceResponse
import io.swagger.v3.oas.annotations.media.Schema

data class UserMyPenaltyListResponse(
    @field:Schema(description = "조회 범위(전체 또는 선택한 기수) 내 페널티 총 횟수", example = "2")
    val penaltyCount: Int,
    @field:Schema(
        description = "조회 범위 내 경고 총 횟수 (동아리 경고 기능 비활성화 시 null)",
        example = "1",
        nullable = true,
    )
    val warningCount: Int?,
    @field:Schema(description = "소속 기수 목록", example = "[6, 7]")
    val cardinals: List<Int>,
    @field:Schema(description = "페널티 목록")
    val penalties: SliceResponse<UserMyPenaltyResponse>,
)
