package com.weeth.domain.attendance.infrastructure

import com.weeth.domain.attendance.application.usecase.command.CloseAttendanceUseCase
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class AttendanceScheduler(
    private val closeAttendanceUseCase: CloseAttendanceUseCase,
) {
    @Scheduled(cron = "0 0 22 * * THU", zone = "Asia/Seoul")
    fun autoCloseAttendance() {
        closeAttendanceUseCase.autoClose()
    }
}
