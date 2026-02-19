package com.weeth.domain.attendance.domain.service

import com.weeth.domain.attendance.domain.repository.AttendanceRepository
import com.weeth.domain.schedule.domain.entity.Meeting
import org.springframework.stereotype.Service

@Service
class AttendanceDeleteService(
    private val attendanceRepository: AttendanceRepository,
) {
    fun deleteAll(meeting: Meeting) {
        attendanceRepository.deleteAllByMeeting(meeting)
    }
}
