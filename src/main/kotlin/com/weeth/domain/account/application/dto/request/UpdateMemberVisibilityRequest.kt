package com.weeth.domain.account.application.dto.request

import io.swagger.v3.oas.annotations.media.Schema

data class UpdateMemberVisibilityRequest(
    @field:Schema(description = "회비 기능 부원 공개 여부 (동아리 전체 적용)", example = "true")
    val visible: Boolean,
)
