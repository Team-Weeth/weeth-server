package com.weeth.domain.releasenote.domain.enums

/**
 * 릴리즈 노트 발행 상태.
 * - [DRAFT]: 작성 중. 공개 API에서 노출되지 않는다.
 * - [PUBLISHED]: 발행됨. 공개 API에서 노출된다.
 */
enum class ReleaseNoteStatus {
    DRAFT,
    PUBLISHED,
}
