package com.weeth.domain.attendance.domain.service

import com.weeth.domain.attendance.domain.entity.Attendance
import com.weeth.domain.attendance.domain.repository.AttendanceRepository
import com.weeth.domain.schedule.domain.entity.Meeting
import com.weeth.domain.user.domain.entity.enums.Status
import org.springframework.stereotype.Service

@Service
class AttendanceGetService(
    private val attendanceRepository: AttendanceRepository,
) {
    fun findAllByMeeting(meeting: Meeting): List<Attendance> = attendanceRepository.findAllByMeetingAndUserStatus(meeting, Status.ACTIVE)
}
