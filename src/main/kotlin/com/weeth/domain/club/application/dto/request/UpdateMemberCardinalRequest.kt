package com.weeth.domain.club.application.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty

data class UpdateMemberCardinalRequest(
    @field:Schema(description = "기수 ID 목록 (최소 1개)", example = "[1, 2, 3]")
    @field:NotEmpty
    val cardinalIds: List<Long>,
    @field:Schema(
        description = "출석 기록이 있는 기수 삭제 시 강제 삭제 여부. 서버가 응답코드 21118을 반환하면 true로 재요청",
        example = "false",
        defaultValue = "false",
    )
    val force: Boolean = false,
)
