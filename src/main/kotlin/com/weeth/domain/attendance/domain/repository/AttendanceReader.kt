package com.weeth.domain.attendance.domain.repository

import com.weeth.domain.attendance.domain.entity.Attendance
import com.weeth.domain.attendance.domain.enums.AttendanceStatus
import com.weeth.domain.session.domain.entity.Session
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice

interface AttendanceReader {
    fun countByClubMemberIdsAndStatus(
        clubMemberIds: List<Long>,
        status: AttendanceStatus,
    ): Long

    fun findByUserIdAndClubIdAndStatus(
        userId: Long,
        clubId: Long,
        status: AttendanceStatus,
        pageable: Pageable,
    ): Slice<Attendance>

    fun findAllBySession(session: Session): List<Attendance>

    fun findBySessionAndUserId(
        session: Session,
        userId: Long,
    ): Attendance?
}
