package com.weeth.domain.attendance.domain.service

import com.weeth.domain.attendance.domain.entity.Session
import com.weeth.domain.attendance.domain.repository.AttendanceRepository
import org.springframework.stereotype.Service

@Service
class AttendanceDeleteService(
    private val attendanceRepository: AttendanceRepository,
) {
    fun deleteAll(session: Session) {
        attendanceRepository.deleteAllBySession(session)
    }
}
