package com.weeth.domain.attendance.application.usecase.command

import com.weeth.domain.attendance.domain.entity.Attendance
import com.weeth.domain.attendance.domain.repository.AttendanceRepository
import com.weeth.domain.schedule.application.exception.MeetingNotFoundException
import com.weeth.domain.schedule.domain.service.MeetingGetService
import com.weeth.domain.user.domain.entity.enums.Status
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class CloseAttendanceUseCase(
    private val meetingGetService: MeetingGetService,
    private val attendanceRepository: AttendanceRepository,
) {
    @Transactional
    fun close(
        now: LocalDate,
        cardinal: Int,
    ) {
        val meetings = meetingGetService.find(cardinal)

        val targetMeeting =
            meetings.firstOrNull { meeting ->
                meeting.start.toLocalDate().isEqual(now) &&
                    meeting.end.toLocalDate().isEqual(now)
            } ?: throw MeetingNotFoundException()

        val attendanceList = attendanceRepository.findAllByMeetingAndUserStatus(targetMeeting, Status.ACTIVE)
        closePendingAttendances(attendanceList)
    }

    @Transactional
    fun autoClose() {
        val meetings = meetingGetService.findAllOpenMeetingsBeforeNow()

        meetings.forEach { meeting ->
            meeting.close()
            val attendanceList = attendanceRepository.findAllByMeetingAndUserStatus(meeting, Status.ACTIVE)
            closePendingAttendances(attendanceList)
        }
    }

    private fun closePendingAttendances(attendances: List<Attendance>) {
        attendances
            .filter { it.isPending }
            .forEach { attendance ->
                attendance.close()
                attendance.user.absent()
            }
    }
}
