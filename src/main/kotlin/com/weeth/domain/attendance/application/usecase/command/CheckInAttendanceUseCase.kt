package com.weeth.domain.attendance.application.usecase.command

import com.weeth.domain.attendance.application.exception.AttendanceCodeMismatchException
import com.weeth.domain.attendance.application.exception.AttendanceNotFoundException
import com.weeth.domain.attendance.domain.enums.Status
import com.weeth.domain.attendance.domain.repository.AttendanceRepository
import com.weeth.domain.user.domain.repository.UserReader
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class CheckInAttendanceUseCase(
    private val userReader: UserReader,
    private val attendanceRepository: AttendanceRepository,
) {
    @Transactional
    fun checkIn(
        userId: Long,
        code: Int,
    ) {
        val user = userReader.getById(userId)
        val now = LocalDateTime.now()

        val todayAttendance =
            attendanceRepository.findCurrentByUserId(userId, now, now.plusMinutes(10))
                ?: throw AttendanceNotFoundException()

        if (todayAttendance.isWrong(code)) {
            throw AttendanceCodeMismatchException()
        }

        if (todayAttendance.status != Status.ATTEND) {
            todayAttendance.attend()
            user.attend()
        }
    }
}
