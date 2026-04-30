package com.weeth.domain.attendance.application.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class AttendanceDetailResponse(
    @field:Schema(description = "출석 횟수", example = "8")
    val attendanceCount: Int,
    @field:Schema(description = "전체 횟수", example = "10")
    val total: Int,
    @field:Schema(description = "결석 횟수", example = "2")
    val absenceCount: Int,
    @field:Schema(description = "출석 내역 목록")
    val attendances: List<AttendanceResponse>,
)
