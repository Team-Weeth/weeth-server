package com.weeth.domain.club.application.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty

/**
 * 여러 멤버에게 동일한 포지션을 한 번에 지정/해제하는 벌크 액션 DTO.
 * [positionOptionId]가 null이면 대상 멤버 전원의 포지션을 해제한다.
 */
data class ClubMemberBulkPositionUpdateRequest(
    @field:Schema(description = "포지션을 일괄 변경할 멤버 ID 목록 (최소 1개)", example = "[20, 21, 22]")
    @field:NotEmpty
    val clubMemberIds: List<Long>,
    @field:Schema(
        description = "지정할 포지션 옵션 ID. null이면 대상 멤버 전원의 포지션을 미지정 상태로 해제합니다.",
        example = "1",
        nullable = true,
    )
    val positionOptionId: Long? = null,
)
