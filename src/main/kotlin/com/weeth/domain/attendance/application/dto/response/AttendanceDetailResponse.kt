package com.weeth.domain.attendance.application.dto.response

data class AttendanceDetailResponse(
    val attendanceCount: Int,
    val total: Int,
    val absenceCount: Int,
    val attendances: List<AttendanceResponse>,
)
