package com.weeth.domain.account.application.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty

data class ExcludePaymentTargetsRequest(
    @field:Schema(description = "납부 대상에서 제외할 납부 대상 ID 목록 (단건은 길이 1)", example = "[1, 2, 3]")
    @field:NotEmpty
    val targetIds: List<Long>,
)
