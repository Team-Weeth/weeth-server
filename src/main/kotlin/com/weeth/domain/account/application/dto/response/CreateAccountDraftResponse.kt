package com.weeth.domain.account.application.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class CreateAccountDraftResponse(
    @field:Schema(description = "회비 장부 ID")
    val accountId: Long,
    @field:Schema(description = "새로 생성된 초안 여부", example = "true")
    val isNew: Boolean,
    @field:Schema(description = "기존 초안의 마지막 수정자 이름. 신규 초안이면 null", nullable = true)
    val lastModifiedByName: String?,
)
