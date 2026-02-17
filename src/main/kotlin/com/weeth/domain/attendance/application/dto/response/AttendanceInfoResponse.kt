package com.weeth.domain.attendance.application.dto.response

import com.weeth.domain.attendance.domain.entity.enums.Status

data class AttendanceInfoResponse(
    val id: Long,
    val status: Status?,
    val name: String?,
    val position: String?,
    val department: String?,
    val studentId: String?,
)
