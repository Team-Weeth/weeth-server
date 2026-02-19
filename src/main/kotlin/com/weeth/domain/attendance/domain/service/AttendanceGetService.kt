package com.weeth.domain.attendance.domain.service

import com.weeth.domain.attendance.domain.entity.Attendance
import com.weeth.domain.attendance.domain.entity.Session
import com.weeth.domain.attendance.domain.repository.AttendanceRepository
import com.weeth.domain.user.domain.entity.enums.Status
import org.springframework.stereotype.Service

@Service
class AttendanceGetService(
    private val attendanceRepository: AttendanceRepository,
) {
    fun findAllByMeeting(session: Session): List<Attendance> = attendanceRepository.findAllBySessionAndUserStatus(session, Status.ACTIVE)
}
