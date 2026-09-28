package com.weeth.domain.attendance.application.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class AttendanceDetailResponse(
    @field:Schema(description = "출석 횟수", example = "8")
    val attendanceCount: Int,
    @field:Schema(description = "출석 + 결석 횟수 (미결 제외)", example = "10")
    val total: Int,
    @field:Schema(description = "결석 횟수", example = "2")
    val absenceCount: Int,
    @field:Schema(description = "출석 내역 목록")
    val attendances: List<AttendanceResponse>,
    @field:Schema(description = "조회한 기수 번호", example = "7")
    val cardinalNumber: Int,
    @field:Schema(description = "선택 기수 출석률. 출석 / (출석 + 결석) × 100, 소수점 버림. 미결 제외, 분모 0이면 0", example = "80")
    val attendanceRate: Int,
)
