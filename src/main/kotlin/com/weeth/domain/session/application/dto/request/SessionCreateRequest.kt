package com.weeth.domain.session.application.dto.request

import com.weeth.domain.session.domain.enums.RecurrenceType
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDate
import java.time.LocalDateTime

data class SessionCreateRequest(
    @field:Schema(description = "세션 제목", example = "1차 정기모임")
    @field:NotBlank
    val title: String,
    @field:Schema(description = "세션 내용", example = "OT 및 자기소개")
    @field:Size(max = 500)
    val content: String?,
    @field:Schema(description = "모임 장소", example = "공학관 401호")
    val location: String?,
    @field:Schema(description = "기수", example = "1")
    val cardinal: Int,
    @field:Schema(description = "시작 시간", example = "2026-03-26T10:00:00")
    val start: LocalDateTime,
    @field:Schema(description = "종료 시간", example = "2026-03-26T22:00:00")
    val end: LocalDateTime,
    @field:Schema(description = "반복 설정 (null=비반복, DAILY/WEEKLY/MONTHLY)")
    val recurrenceType: RecurrenceType?,
    @field:Schema(description = "반복 종료일 (반복 설정 시 필수, 시작일 기준 최대 1년)", example = "2026-06-30")
    val recurrenceEndDate: LocalDate?,
)
