package com.weeth.domain.session.application.dto.response

import com.weeth.domain.session.domain.enums.RecurrenceType
import com.weeth.domain.session.domain.enums.SessionGroupStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

data class SessionGroupResponse(
    @field:Schema(description = "반복 그룹 ID (null이면 비반복)")
    val groupId: Long?,
    @field:Schema(description = "세션 제목")
    val title: String,
    @field:Schema(description = "반복 설정 (null이면 비반복)")
    val recurrenceType: RecurrenceType?,
    @field:Schema(description = "반복 설명 텍스트 (예: '매주 수요일 19시')")
    val recurrenceDescription: String?,
    @field:Schema(description = "그룹 첫 세션 시작일")
    val startDate: LocalDate?,
    @field:Schema(description = "반복 종료일")
    val endDate: LocalDate?,
    @field:Schema(description = "완료(CLOSED) 세션 수")
    val completedCount: Int,
    @field:Schema(description = "전체 세션 수")
    val totalCount: Int,
    @field:Schema(description = "그룹 상태")
    val status: SessionGroupStatus,
    @field:Schema(description = "세션 목록")
    val sessions: List<SessionInfoResponse>,
)
