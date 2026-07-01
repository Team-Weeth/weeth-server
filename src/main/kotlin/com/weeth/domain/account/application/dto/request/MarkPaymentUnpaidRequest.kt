package com.weeth.domain.account.application.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty

data class MarkPaymentUnpaidRequest(
    @field:Schema(description = "납부를 정정(취소)할 납부 대상 ID 목록 (단건은 길이 1)", example = "[1, 2, 3]")
    @field:NotEmpty
    val targetIds: List<Long>,
)
