package com.weeth.domain.account.application.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

data class SaveAccountCarryOverRequest(
    @field:Schema(description = "이월 활성화 여부", example = "true")
    val enabled: Boolean,
    @field:Schema(description = "이월 금액 (원). enabled=true 일 때 필수", example = "152129", nullable = true)
    val amount: Int?,
    @field:Schema(description = "이월 메모. 사용자가 입력하지 않은 경우는 'O기 이월 금액입니다.'를 넣어주세요.", example = "4기 잔액", nullable = true)
    @field:Size(max = 30)
    val memo: String?,
)
