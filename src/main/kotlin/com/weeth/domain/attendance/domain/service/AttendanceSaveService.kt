package com.weeth.domain.attendance.domain.service

import com.weeth.domain.attendance.domain.entity.Attendance
import com.weeth.domain.attendance.domain.entity.Session
import com.weeth.domain.attendance.domain.repository.AttendanceRepository
import com.weeth.domain.user.domain.entity.User
import org.springframework.stereotype.Service

@Service
class AttendanceSaveService(
    private val attendanceRepository: AttendanceRepository,
) {
    fun init(
        user: User,
        sessions: List<Session>?,
    ) {
        sessions?.forEach { session ->
            attendanceRepository.save(Attendance.create(session, user))
        }
    }

    fun saveAll(
        userList: List<User>,
        session: Session,
    ) {
        val attendances = userList.map { user -> Attendance.create(session, user) }
        attendanceRepository.saveAll(attendances)
    }
}
