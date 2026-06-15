package com.weeth.domain.account.application.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class CreateAccountDraftResponse(
    @field:Schema(description = "회비 장부 ID")
    val accountId: Long,
    @field:Schema(
        description =
            "새로 생성된 초안 여부. false면 작성 중인 초안이 이미 있으므로 " +
                "'이어서 작성 / 새로 작성' 분기를 노출해주세요. " +
                "이어서 작성 시 등록 현황 조회 API로 폼을 복원하고, 새로 작성 시 초안 폐기 API 호출 후 본 API를 재호출해주세요.",
        example = "true",
    )
    val isNew: Boolean,
    @field:Schema(
        description = "기존 초안의 마지막 수정자 이름. '00님이 작성 중인 회비가 있어요' 안내 문구에 사용해주세요. 신규 초안이면 null",
        nullable = true,
    )
    val lastModifiedByName: String?,
)
