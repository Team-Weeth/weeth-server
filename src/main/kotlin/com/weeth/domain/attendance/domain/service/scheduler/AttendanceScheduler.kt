package com.weeth.domain.attendance.domain.service.scheduler

import com.weeth.domain.attendance.domain.repository.AttendanceRepository
import com.weeth.domain.schedule.domain.service.MeetingGetService
import com.weeth.domain.user.domain.entity.enums.Status
import org.springframework.transaction.annotation.Transactional
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class AttendanceScheduler(
    private val meetingGetService: MeetingGetService,
    private val attendanceRepository: AttendanceRepository,
) {
    @Transactional
    @Scheduled(cron = "0 0 22 * * THU", zone = "Asia/Seoul")
    fun autoCloseAttendance() {
        val meetings = meetingGetService.findAllOpenMeetingsBeforeNow()

        meetings.forEach { meeting ->
            meeting.close()
            val attendanceList = attendanceRepository.findAllByMeetingAndUserStatus(meeting, Status.ACTIVE)
            attendanceList
                .filter { it.isPending }
                .forEach { attendance ->
                    attendance.close()
                    attendance.user.absent()
                }
        }
    }
}
