package com.weeth.domain.account.application.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class AccountCarryOverSourceResponse(
    @field:Schema(
        description =
            "이전 기수 활성 장부 존재 여부. true면 잔액 배너를 노출하고 이월 금액으로 balance를 사용해주세요. " +
                "false면 '이전 기수 정보가 없습니다' 안내와 함께 금액 직접 입력 UI를 노출해주세요.",
        example = "true",
    )
    val hasPreviousAccount: Boolean,
    @field:Schema(description = "이전 기수. 이전 장부가 없으면 null", example = "3", nullable = true)
    val cardinalNumber: Int?,
    @field:Schema(description = "이전 기수 장부 잔액 (원). 이전 장부가 없으면 null", example = "240000", nullable = true)
    val balance: Int?,
)
