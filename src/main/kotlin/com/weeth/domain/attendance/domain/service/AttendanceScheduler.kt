package com.weeth.domain.attendance.domain.service

import com.weeth.domain.schedule.domain.service.MeetingGetService
import jakarta.transaction.Transactional
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class AttendanceScheduler(
    private val meetingGetService: MeetingGetService,
    private val attendanceGetService: AttendanceGetService,
    private val attendanceUpdateService: AttendanceUpdateService,
) {
    @Transactional
    @Scheduled(cron = "0 0 22 * * THU", zone = "Asia/Seoul")
    fun autoCloseAttendance() {
        val meetings = meetingGetService.findAllOpenMeetingsBeforeNow()

        meetings.forEach { meeting ->
            meeting.close()
            val attendanceList = attendanceGetService.findAllByMeeting(meeting)
            attendanceUpdateService.close(attendanceList)
        }
    }
}
