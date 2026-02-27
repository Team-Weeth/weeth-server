package com.weeth.domain.attendance.infrastructure

import com.weeth.domain.attendance.application.usecase.command.ManageAttendanceUseCase
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class AttendanceScheduler(
    private val manageAttendanceUseCase: ManageAttendanceUseCase,
) {
    @Scheduled(cron = "0 0 22 * * THU", zone = "Asia/Seoul")
    fun autoCloseAttendance() {
        manageAttendanceUseCase.autoClose()
    }
}
