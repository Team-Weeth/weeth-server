package com.weeth.domain.account.application.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size

data class RefundPaymentRequest(
    @field:Schema(description = "환불 처리할 납부 대상 ID 목록 (단건은 길이 1). 납부 완료 상태만 가능", example = "[1, 2]")
    @field:NotEmpty
    val targetIds: List<Long>,
    @field:Schema(description = "환불 거래에 남길 메모", nullable = true)
    @field:Size(max = 200)
    val memo: String? = null,
)
