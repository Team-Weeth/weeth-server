package com.weeth.domain.attendance.domain.repository

import com.weeth.domain.attendance.domain.enums.AttendanceStatus

interface AttendanceReader {
    fun countByClubMemberIdsAndStatus(
        clubMemberIds: List<Long>,
        status: AttendanceStatus,
    ): Long
}
