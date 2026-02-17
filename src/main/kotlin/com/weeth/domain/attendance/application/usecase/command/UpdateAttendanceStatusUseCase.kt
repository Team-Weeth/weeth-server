package com.weeth.domain.attendance.application.usecase.command

import com.weeth.domain.attendance.application.dto.request.UpdateAttendanceStatusRequest
import com.weeth.domain.attendance.application.exception.AttendanceNotFoundException
import com.weeth.domain.attendance.domain.entity.enums.Status
import com.weeth.domain.attendance.domain.repository.AttendanceRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UpdateAttendanceStatusUseCase(
    private val attendanceRepository: AttendanceRepository,
) {
    @Transactional
    fun execute(attendanceUpdates: List<UpdateAttendanceStatusRequest>) {
        attendanceUpdates.forEach { update ->
            val attendance =
                attendanceRepository.findByIdOrNull(update.attendanceId)
                    ?: throw AttendanceNotFoundException()
            val user = attendance.user
            val newStatus = Status.valueOf(update.status)

            if (newStatus == Status.ABSENT) {
                attendance.close()
                user.removeAttend()
                user.absent()
            } else {
                attendance.attend()
                user.removeAbsent()
                user.attend()
            }
        }
    }
}
