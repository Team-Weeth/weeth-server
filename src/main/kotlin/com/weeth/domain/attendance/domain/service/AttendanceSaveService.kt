package com.weeth.domain.attendance.domain.service

import com.weeth.domain.attendance.domain.entity.Attendance
import com.weeth.domain.attendance.domain.repository.AttendanceRepository
import com.weeth.domain.schedule.domain.entity.Meeting
import com.weeth.domain.user.domain.entity.User
import org.springframework.stereotype.Service

@Service
class AttendanceSaveService(
    private val attendanceRepository: AttendanceRepository,
) {
    fun init(
        user: User,
        meetings: List<Meeting>?,
    ) {
        meetings?.forEach { meeting ->
            attendanceRepository.save(Attendance(meeting, user))
        }
    }

    fun saveAll(
        userList: List<User>,
        meeting: Meeting,
    ) {
        val attendances = userList.map { user -> Attendance(meeting, user) }
        attendanceRepository.saveAll(attendances)
    }
}
