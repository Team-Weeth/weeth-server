package com.weeth.domain.attendance.application.dto.response

import com.weeth.domain.attendance.domain.entity.enums.AttendanceStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class AttendanceSummaryResponse(
    @field:Schema(description = "출석률", example = "80")
    val attendanceRate: Int?,
    @field:Schema(description = "정기모임 제목", example = "1주차 정기모임")
    val title: String?,
    @field:Schema(description = "출석 상태", example = "ATTEND")
    val status: AttendanceStatus?,
    @field:Schema(description = "어드민인 경우 출석 코드 노출", example = "1234")
    val code: Int?,
    @field:Schema(description = "정기모임 시작 시간")
    val start: LocalDateTime?,
    @field:Schema(description = "정기모임 종료 시간")
    val end: LocalDateTime?,
    @field:Schema(description = "정기모임 장소", example = "공학관 401호")
    val location: String?,
)
