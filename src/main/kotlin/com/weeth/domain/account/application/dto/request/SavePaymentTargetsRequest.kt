package com.weeth.domain.account.application.dto.request

import io.swagger.v3.oas.annotations.media.Schema

data class SavePaymentTargetsRequest(
    @field:Schema(
        description =
            "납부 대상으로 선택한 동아리 회원 ID 목록(스냅샷). " +
                "해당 기수 명부 중 이 목록에 없는 회원은 자동으로 제외 처리됩니다. " +
                "빈 목록이면 전원 제외를 의미합니다.",
        example = "[1, 2, 3]",
    )
    val targetedClubMemberIds: List<Long> = emptyList(),
)
