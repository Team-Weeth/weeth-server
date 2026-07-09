package com.weeth.domain.account.application.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class AccountVisibilityResponse(
    @field:Schema(description = "회비 기능 부원 공개 여부. false면 프론트에서 회비 탭을 숨긴다.", example = "true")
    val visible: Boolean,
)
