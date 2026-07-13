package com.weeth.domain.attendance.domain.repository

import com.weeth.domain.attendance.domain.entity.Attendance
import com.weeth.domain.attendance.domain.enums.AttendanceStatus
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice

interface AttendanceReader {
    fun countByClubMemberIdsAndStatus(
        clubMemberIds: List<Long>,
        status: AttendanceStatus,
    ): Long

    fun findByUserIdAndStatus(
        userId: Long,
        status: AttendanceStatus,
        pageable: Pageable,
    ): Slice<Attendance>
}
