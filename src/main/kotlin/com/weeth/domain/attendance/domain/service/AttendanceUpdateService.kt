package com.weeth.domain.attendance.domain.service

import com.weeth.domain.attendance.domain.entity.Attendance
import com.weeth.domain.attendance.domain.entity.enums.AttendanceStatus
import org.springframework.stereotype.Service

@Service
class AttendanceUpdateService {
    fun attend(attendance: Attendance) {
        attendance.attend()
        attendance.user.attend()
    }

    fun close(attendances: List<Attendance>) {
        attendances
            .filter { it.isPending() }
            .forEach { attendance ->
                attendance.close()
                attendance.user.absent()
            }
    }

    fun updateUserAttendanceByStatus(attendances: List<Attendance>) {
        attendances.forEach { attendance ->
            val user = attendance.user
            if (attendance.status == AttendanceStatus.ATTEND) {
                user.removeAttend()
            } else {
                user.removeAbsent()
            }
        }
    }
}
