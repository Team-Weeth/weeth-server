package com.weeth.domain.account.application.dto.request

import io.swagger.v3.oas.annotations.media.Schema

data class UpdateMemberVisibilityRequest(
    @field:Schema(description = "부원 거래 내역 공개 여부", example = "true")
    val visible: Boolean,
)
