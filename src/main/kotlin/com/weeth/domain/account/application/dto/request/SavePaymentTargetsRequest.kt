package com.weeth.domain.account.application.dto.request

import io.swagger.v3.oas.annotations.media.Schema

data class SavePaymentTargetsRequest(
    @field:Schema(
        description = "납부 대상으로 지정할 동아리 회원 ID 목록. 두 목록에 모두 없는 회원의 기존 상태는 유지됩니다.",
        example = "[1, 2, 3]",
    )
    val targetedClubMemberIds: List<Long> = emptyList(),
    @field:Schema(
        description = "납부 대상에서 제외할 동아리 회원 ID 목록. 두 목록에 모두 없는 회원의 기존 상태는 유지됩니다.",
        example = "[4, 5]",
    )
    val excludedClubMemberIds: List<Long> = emptyList(),
)
