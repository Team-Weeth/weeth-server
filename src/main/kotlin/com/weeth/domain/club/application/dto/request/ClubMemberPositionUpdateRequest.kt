package com.weeth.domain.club.application.dto.request

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 멤버 포지션 지정/해제 전용 단일 목적 액션 DTO.
 *
 * 주의: 필드가 nullable이라 `mapper-dto.md`의 PATCH 부분수정 컨벤션(null=변경 안 함, 여러 필드를 가진
 * 전체 엔티티 업데이트용 — 예: [ClubUpdateRequest])처럼 보이지만 그 패턴이 아니다. 이 DTO는
 * [ClubMemberRoleUpdateRequest]처럼 단일 필드로 단일 액션을 수행하는 DTO이며, [positionOptionId]가
 * null이면 "값을 보내지 않았으니 기존 값 유지"가 아니라 "포지션을 해제하라"는 명시적 액션을 의미한다.
 */
data class ClubMemberPositionUpdateRequest(
    @field:Schema(
        description = "지정할 포지션 옵션 ID. null이면 포지션을 미지정 상태로 해제합니다 (변경 안 함이 아닌 명시적 해제 액션).",
        example = "1",
        nullable = true,
    )
    val positionOptionId: Long? = null,
)
