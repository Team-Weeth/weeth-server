package com.weeth.domain.attendance.application.usecase.command

import com.weeth.domain.attendance.application.exception.AttendanceCodeMismatchException
import com.weeth.domain.attendance.application.exception.AttendanceNotFoundException
import com.weeth.domain.attendance.domain.entity.enums.Status
import com.weeth.domain.user.domain.service.UserGetService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class CheckInAttendanceUseCase(
    private val userGetService: UserGetService,
) {
    @Transactional
    fun execute(
        userId: Long,
        code: Int,
    ) {
        val user = userGetService.find(userId)
        val now = LocalDateTime.now()

        val todayAttendance =
            user.attendances.firstOrNull { attendance ->
                attendance.meeting.start
                    .minusMinutes(10)
                    .isBefore(now) &&
                    attendance.meeting.end.isAfter(now)
            } ?: throw AttendanceNotFoundException()

        if (todayAttendance.isWrong(code)) {
            throw AttendanceCodeMismatchException()
        }

        if (todayAttendance.status != Status.ATTEND) {
            todayAttendance.attend()
            user.attend()
        }
    }
}
